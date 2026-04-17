package com.artistas.api.service;

import com.artistas.api.domain.Artist;
import com.artistas.api.dto.PageResponse;
import com.artistas.api.dto.album.AlbumSummaryResponse;
import com.artistas.api.dto.artist.ArtistDetailResponse;
import com.artistas.api.dto.artist.ArtistRequest;
import com.artistas.api.dto.artist.ArtistResponse;
import com.artistas.api.repository.AlbumRepository;
import com.artistas.api.repository.ArtistRepository;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Casos de uso de artistas: listagem paginada com contagem agregada de álbuns, detalhe com resumo
 * de álbuns, criação e atualização. A contagem por página usa uma query em lote para evitar N+1.
 */
@Service
@RequiredArgsConstructor
public class ArtistService {

    private final ArtistRepository artistRepository;
    private final AlbumRepository albumRepository;

    /**
     * Lista artistas com busca opcional por nome e ordenação por nome.
     * <p>Regra técnica: quando não há filtro de nome, usa {@code findAll} em vez de JPQL com parâmetro
     * {@code null}, evitando incompatibilidade de tipo no PostgreSQL ({@code lower(bytea)}).</p>
     *
     * @param nameFilter    substring do nome (case-insensitive), ou vazio para listar todos
     * @param sortDirection {@code asc} ou {@code desc}
     */
    @Transactional(readOnly = true)
    public PageResponse<ArtistResponse> list(String nameFilter, String sortDirection, int page, int size) {
        String name = (nameFilter == null || nameFilter.isBlank()) ? null : nameFilter.trim();
        Sort sort = "desc".equalsIgnoreCase(sortDirection)
                ? Sort.by("name").descending()
                : Sort.by("name").ascending();
        Pageable pageable = PageRequest.of(page, size, sort);
        // Evita JPQL (:name IS NULL OR …) com parâmetro nulo: no PostgreSQL o bind virava bytea e lower(bytea) falhava.
        Page<Artist> result = name == null
                ? artistRepository.findAll(pageable)
                : artistRepository.findByNameContainingIgnoreCase(name, pageable);
        List<Long> ids = result.getContent().stream().map(Artist::getId).toList();
        Map<Long, Long> counts = albumCounts(ids);
        List<ArtistResponse> mapped = result.getContent().stream()
                .map(a -> new ArtistResponse(a.getId(), a.getName(), counts.getOrDefault(a.getId(), 0L)))
                .toList();
        return new PageResponse<>(
                mapped,
                result.getNumber(),
                result.getSize(),
                result.getTotalElements(),
                result.getTotalPages(),
                result.isLast()
        );
    }

    /**
     * Agrega quantidade de álbuns por artista em uma única query ({@code GROUP BY}).
     *
     * @param artistIds ids retornados na página atual (pode ser vazio)
     */
    private Map<Long, Long> albumCounts(List<Long> artistIds) {
        if (artistIds.isEmpty()) {
            return Map.of();
        }
        Map<Long, Long> map = new HashMap<>();
        for (Object[] row : albumRepository.countAlbumsByArtistIds(artistIds)) {
            map.put((Long) row[0], (Long) row[1]);
        }
        return map;
    }

    /**
     * Detalhe do artista com lista de álbuns (resumo: id, título, quantidade de capas).
     *
     * @param id id do artista
     */
    @Transactional(readOnly = true)
    public ArtistDetailResponse getById(Long id) {
        Artist artist = artistRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Artista não encontrado"));
        return toDetail(artist);
    }

    /** Mapeia entidade carregada para DTO de detalhe (usa coleção {@code albums} do artista). */
    private ArtistDetailResponse toDetail(Artist artist) {
        var albums = artist.getAlbums().stream()
                .map(al -> new AlbumSummaryResponse(
                        al.getId(),
                        al.getTitle(),
                        al.getCovers().size()))
                .collect(Collectors.toList());
        return new ArtistDetailResponse(artist.getId(), artist.getName(), albums);
    }

    /**
     * Cria artista com nome único na validação de tamanho; contagem de álbuns inicial é zero.
     */
    @Transactional
    public ArtistResponse create(ArtistRequest request) {
        Artist saved = artistRepository.save(Artist.builder().name(request.name().trim()).build());
        return new ArtistResponse(saved.getId(), saved.getName(), 0);
    }

    /**
     * Atualiza o nome e devolve a contagem atual de álbuns do artista.
     */
    @Transactional
    public ArtistResponse update(Long id, ArtistRequest request) {
        Artist artist = artistRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Artista não encontrado"));
        artist.setName(request.name().trim());
        long count = albumRepository.countByArtistId(id);
        return new ArtistResponse(artist.getId(), artist.getName(), count);
    }
}
