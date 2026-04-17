package com.artistas.api.dto.artist;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Artista com contagem de álbuns (listagem)")
public record ArtistResponse(
        Long id,
        String name,
        long albumCount
) {
}
