package com.artistas.api.dto.auth;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;

@Schema(description = "Renovação do access token")
public record RefreshRequest(
        @NotBlank String refreshToken
) {
}
