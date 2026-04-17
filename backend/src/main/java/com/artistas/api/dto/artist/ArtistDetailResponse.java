package com.artistas.api.dto.artist;

import com.artistas.api.dto.album.AlbumSummaryResponse;
import io.swagger.v3.oas.annotations.media.Schema;

import java.util.List;

@Schema(description = "Artista com álbuns")
public record ArtistDetailResponse(
        Long id,
        String name,
        List<AlbumSummaryResponse> albums
) {
}
