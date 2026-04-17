package com.artistas.api.service;

import com.artistas.api.domain.Album;
import com.artistas.api.domain.AlbumCover;
import com.artistas.api.domain.Artist;
import com.artistas.api.dto.PageResponse;
import com.artistas.api.dto.album.AlbumCreatedEvent;
import com.artistas.api.dto.album.AlbumDetailResponse;
import com.artistas.api.dto.album.AlbumRequest;
import com.artistas.api.dto.album.CoverResponse;
import com.artistas.api.repository.AlbumCoverRepository;
import com.artistas.api.repository.AlbumRepository;
import com.artistas.api.repository.ArtistRepository;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/**
 * Casos de uso de álbuns: busca paginada, detalhe com capas (URLs pré-assinadas), CRUD e upload
 * multipart para o armazenamento S3. Ao criar um álbum, publica evento STOMP em {@code /topic/albums}
 * para notificar clientes conectados.
 */
@Service
@RequiredArgsConstructor
public class AlbumService {

    private final AlbumRepository albumRepository;
    private final AlbumCoverRepository albumCoverRepository;
    private final ArtistRepository artistRepository;
    private final StorageService storageService;
    private final SimpMessagingTemplate messagingTemplate;

    /**
     * Lista álbuns com filtros opcionais, sem gerar URLs pré-assinadas (lista de capas vazia; apenas {@code coverCount}).
     * <p>Uso: listagens onde o custo de assinatura S3 por item não é desejado.</p>
     *
     * @param artistId   filtra por artista, ou {@code null} para todos
     * @param titleContains trecho do título (ILIKE), ou vazio/{@code null} para ignorar
     * @param page índice base zero
     * @param size itens por página
     */
    @Transactional(readOnly = true)
    public PageResponse<AlbumDetailResponse> search(Long artistId, String titleContains, int page, int size) {
        String title = (titleContains == null || titleContains.isBlank()) ? null : titleContains.trim();
        Pageable pageable = PageRequest.of(page, size);
        Page<Album> result = albumRepository.search(artistId, title, pageable);
        Page<AlbumDetailResponse> mapped = result.map(this::toDetailWithoutPresign);
        return PageResponse.from(mapped);
    }

    /**
     * Igual a {@link #search(Long, String, int, int)}, porém preenche cada item com URLs GET pré-assinadas
     * para todas as capas. Pode ser custoso em páginas grandes (N chamadas ao presigner).
     */
    @Transactional(readOnly = true)
    public PageResponse<AlbumDetailResponse> searchWithCovers(Long artistId, String titleContains, int page, int size) {
        String title = (titleContains == null || titleContains.isBlank()) ? null : titleContains.trim();
        Pageable pageable = PageRequest.of(page, size);
        Page<Album> result = albumRepository.search(artistId, title, pageable);
        Page<AlbumDetailResponse> mapped = result.map(this::toDetailWithPresign);
        return PageResponse.from(mapped);
    }

    /**
     * Detalhe do álbum por id, sempre com capas e URLs pré-assinadas.
     *
     * @param id identificador do álbum
     * @throws EntityNotFoundException se não existir
     */
    @Transactional(readOnly = true)
    public AlbumDetailResponse getById(Long id) {
        Album album = albumRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Álbum não encontrado"));
        return toDetailWithPresign(album);
    }

    /** Monta DTO de detalhe sem acessar o S3 (contagem de capas via {@code COUNT}). */
    private AlbumDetailResponse toDetailWithoutPresign(Album album) {
        int cc = (int) albumCoverRepository.countByAlbumId(album.getId());
        return new AlbumDetailResponse(
                album.getId(),
                album.getArtist().getId(),
                album.getArtist().getName(),
                album.getTitle(),
                cc,
                List.of()
        );
    }

    /** Monta DTO com lista ordenada de capas e URL pré-assinada por objeto no bucket. */
    private AlbumDetailResponse toDetailWithPresign(Album album) {
        List<CoverResponse> covers = new ArrayList<>();
        for (AlbumCover c : albumCoverRepository.findByAlbumIdOrderBySortOrderAsc(album.getId())) {
            String url = storageService.presignGet(c.getObjectKey());
            covers.add(new CoverResponse(c.getId(), c.getContentType(), url, c.getSortOrder()));
        }
        return new AlbumDetailResponse(
                album.getId(),
                album.getArtist().getId(),
                album.getArtist().getName(),
                album.getTitle(),
                covers.size(),
                covers
        );
    }

    /**
     * Cria álbum vinculado a um artista existente e envia {@link AlbumCreatedEvent} ao broker WebSocket.
     *
     * @param request artista e título (validados no controller)
     */
    @Transactional
    public AlbumDetailResponse create(AlbumRequest request) {
        Artist artist = artistRepository.findById(request.artistId())
                .orElseThrow(() -> new EntityNotFoundException("Artista não encontrado"));
        Album album = Album.builder()
                .artist(artist)
                .title(request.title().trim())
                .build();
        album = albumRepository.save(album);
        AlbumDetailResponse detail = toDetailWithPresign(album);
        messagingTemplate.convertAndSend("/topic/albums", new AlbumCreatedEvent(
                album.getId(),
                artist.getId(),
                artist.getName(),
                album.getTitle()
        ));
        return detail;
    }

    /**
     * Atualiza artista e título do álbum.
     *
     * @param id id do álbum
     * @param request novo artista e título
     */
    @Transactional
    public AlbumDetailResponse update(Long id, AlbumRequest request) {
        Album album = albumRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Álbum não encontrado"));
        Artist artist = artistRepository.findById(request.artistId())
                .orElseThrow(() -> new EntityNotFoundException("Artista não encontrado"));
        album.setArtist(artist);
        album.setTitle(request.title().trim());
        return toDetailWithPresign(album);
    }

    /**
     * Faz upload de um ou mais arquivos para o bucket, persiste metadados em {@code album_cover}
     * e retorna as capas com URL pré-assinada. Regra de negócio: {@code sortOrder} continua a sequência
     * existente do álbum (max + 1 por arquivo).
     *
     * @param albumId álbum alvo
     * @param files   multipart {@code files}; entradas vazias são ignoradas
     * @return capas criadas, na ordem processada
     */
    @Transactional
    public List<CoverResponse> addCovers(Long albumId, MultipartFile[] files) throws IOException {
        Album album = albumRepository.findById(albumId)
                .orElseThrow(() -> new EntityNotFoundException("Álbum não encontrado"));
        int order = (int) albumCoverRepository.findByAlbumIdOrderBySortOrderAsc(albumId).stream()
                .mapToInt(AlbumCover::getSortOrder)
                .max()
                .orElse(-1) + 1;
        List<CoverResponse> out = new ArrayList<>();
        if (files == null) {
            return out;
        }
        for (MultipartFile file : files) {
            if (file == null || file.isEmpty()) {
                continue;
            }
            String key = storageService.uploadAlbumCover(albumId, file);
            AlbumCover cover = AlbumCover.builder()
                    .album(album)
                    .objectKey(key)
                    .contentType(file.getContentType())
                    .sortOrder(order++)
                    .build();
            cover = albumCoverRepository.save(cover);
            out.add(new CoverResponse(
                    cover.getId(),
                    cover.getContentType(),
                    storageService.presignGet(key),
                    cover.getSortOrder()
            ));
        }
        return out;
    }
}
