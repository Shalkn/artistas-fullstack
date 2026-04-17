package com.artistas.api.dto.album;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

@Schema(description = "Criação/atualização de álbum")
public record AlbumRequest(
        @NotNull Long artistId,
        @NotBlank @Size(max = 500) String title
) {
}
