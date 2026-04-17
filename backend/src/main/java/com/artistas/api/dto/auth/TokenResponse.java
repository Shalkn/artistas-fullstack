package com.artistas.api.dto.auth;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Tokens JWT (access expira em 5 min por padrão; usar refresh para renovar)")
public record TokenResponse(
        String accessToken,
        String refreshToken,
        long expiresInSeconds
) {
}
