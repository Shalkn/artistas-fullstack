package com.artistas.api.dto.album;

import io.swagger.v3.oas.annotations.media.Schema;

import java.util.List;

@Schema(description = "Álbum com capas (URLs pré-assinadas quando solicitado)")
public record AlbumDetailResponse(
        Long id,
        Long artistId,
        String artistName,
        String title,
        int coverCount,
        List<CoverResponse> covers
) {
}
