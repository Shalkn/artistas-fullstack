package com.artistas.api.dto.album;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Evento WebSocket: novo álbum")
public record AlbumCreatedEvent(
        Long albumId,
        Long artistId,
        String artistName,
        String albumTitle
) {
}
