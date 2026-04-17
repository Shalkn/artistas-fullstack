package com.artistas.api.dto.album;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Capa com URL pré-assinada (expira em 30 min)")
public record CoverResponse(
        Long id,
        String contentType,
        String presignedUrl,
        int sortOrder
) {
}
