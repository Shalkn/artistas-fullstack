package com.artistas.api.dto.album;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Resumo de álbum")
public record AlbumSummaryResponse(
        Long id,
        String title,
        int coverCount
) {
}
