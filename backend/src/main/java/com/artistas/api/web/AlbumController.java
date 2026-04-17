package com.artistas.api.web;

import com.artistas.api.dto.PageResponse;
import com.artistas.api.dto.album.AlbumDetailResponse;
import com.artistas.api.dto.album.AlbumRequest;
import com.artistas.api.dto.album.CoverResponse;
import com.artistas.api.service.AlbumService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.List;

/** Consulta, CRUD de álbuns e upload multipart de capas; notificação WS ocorre no serviço ao criar. */
@RestController
@RequestMapping("/api/v1/albums")
@RequiredArgsConstructor
@Tag(name = "Álbuns")
@SecurityRequirement(name = "bearer-jwt")
public class AlbumController {

    private final AlbumService albumService;

    @GetMapping
    @Operation(summary = "Consulta parametrizada de álbuns (artistId, título, paginação)")
    public ResponseEntity<PageResponse<AlbumDetailResponse>> search(
            @RequestParam(required = false) Long artistId,
            @RequestParam(required = false) String title,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(defaultValue = "false") boolean includePresignedCovers
    ) {
        if (includePresignedCovers) {
            return ResponseEntity.ok(albumService.searchWithCovers(artistId, title, page, size));
        }
        return ResponseEntity.ok(albumService.search(artistId, title, page, size));
    }

    @GetMapping("/{id}")
    @Operation(summary = "Detalhe do álbum com capas (URLs pré-assinadas 30 min)")
    public ResponseEntity<AlbumDetailResponse> get(@PathVariable Long id) {
        return ResponseEntity.ok(albumService.getById(id));
    }

    @PostMapping
    @Operation(summary = "Criar álbum (dispara notificação WebSocket)")
    public ResponseEntity<AlbumDetailResponse> create(@Valid @RequestBody AlbumRequest request) {
        return ResponseEntity.ok(albumService.create(request));
    }

    @PutMapping("/{id}")
    @Operation(summary = "Atualizar álbum")
    public ResponseEntity<AlbumDetailResponse> update(@PathVariable Long id, @Valid @RequestBody AlbumRequest request) {
        return ResponseEntity.ok(albumService.update(id, request));
    }

    @PostMapping(value = "/{id}/covers", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @Operation(summary = "Upload de uma ou mais capas (MinIO / S3)")
    public ResponseEntity<List<CoverResponse>> uploadCovers(
            @PathVariable Long id,
            @RequestParam("files") MultipartFile[] files
    ) throws IOException {
        return ResponseEntity.ok(albumService.addCovers(id, files));
    }
}
