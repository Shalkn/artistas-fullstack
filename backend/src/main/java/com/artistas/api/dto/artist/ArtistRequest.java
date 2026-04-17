package com.artistas.api.dto.artist;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

@Schema(description = "Criação/atualização de artista")
public record ArtistRequest(
        @NotBlank @Size(max = 255) String name
) {
}
