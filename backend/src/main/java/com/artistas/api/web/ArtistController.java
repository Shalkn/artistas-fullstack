package com.artistas.api.web;

import com.artistas.api.dto.PageResponse;
import com.artistas.api.dto.artist.ArtistDetailResponse;
import com.artistas.api.dto.artist.ArtistRequest;
import com.artistas.api.dto.artist.ArtistResponse;
import com.artistas.api.service.ArtistService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

/** CRUD e listagem paginada de artistas; todas as rotas exigem JWT válido. */
@RestController
@RequestMapping("/api/v1/artists")
@RequiredArgsConstructor
@Tag(name = "Artistas")
@SecurityRequirement(name = "bearer-jwt")
public class ArtistController {

    private final ArtistService artistService;

    @GetMapping
    @Operation(summary = "Listar artistas (paginação, busca por nome, ordenação asc/desc)")
    public ResponseEntity<PageResponse<ArtistResponse>> list(
            @RequestParam(required = false) String name,
            @RequestParam(defaultValue = "asc") String sort,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size
    ) {
        return ResponseEntity.ok(artistService.list(name, sort, page, size));
    }

    @GetMapping("/{id}")
    @Operation(summary = "Detalhe do artista com álbuns")
    public ResponseEntity<ArtistDetailResponse> get(@PathVariable Long id) {
        return ResponseEntity.ok(artistService.getById(id));
    }

    @PostMapping
    @Operation(summary = "Criar artista")
    public ResponseEntity<ArtistResponse> create(@Valid @RequestBody ArtistRequest request) {
        return ResponseEntity.ok(artistService.create(request));
    }

    @PutMapping("/{id}")
    @Operation(summary = "Atualizar artista")
    public ResponseEntity<ArtistResponse> update(@PathVariable Long id, @Valid @RequestBody ArtistRequest request) {
        return ResponseEntity.ok(artistService.update(id, request));
    }
}
