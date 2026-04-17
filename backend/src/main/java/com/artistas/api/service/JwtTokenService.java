package com.artistas.api.service;

import com.artistas.api.config.AppProperties;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.springframework.stereotype.Service;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Date;

/**
 * Cria e valida JWT de <strong>acesso</strong> (HS256). O segredo é obtido de {@code app.jwt.secret};
 * se tiver menos de 32 caracteres, é expandido deterministicamente para satisfazer o tamanho mínimo da chave.
 */
@Service
public class JwtTokenService {

    private final AppProperties appProperties;
    private final SecretKey secretKey;

    /**
     * @param appProperties configuração com segredo e tempo de vida do access token
     */
    public JwtTokenService(AppProperties appProperties) {
        this.appProperties = appProperties;
        String secret = appProperties.getJwt().getSecret();
        if (secret.length() < 32) {
            secret = String.format("%-32s", secret).substring(0, 32);
        }
        this.secretKey = Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
    }

    /**
     * Gera JWT assinado com {@code sub = username} e expiração conforme {@code accessTokenMinutes}.
     *
     * @param username identificador único do usuário na API
     */
    public String createAccessToken(String username) {
        Instant now = Instant.now();
        Instant exp = now.plusSeconds(appProperties.getJwt().getAccessTokenMinutes() * 60L);
        return Jwts.builder()
                .subject(username)
                .issuedAt(Date.from(now))
                .expiration(Date.from(exp))
                .signWith(secretKey)
                .compact();
    }

    /**
     * Valida assinatura e extrai o subject (nome de usuário). Lança exceção de JWT se inválido ou expirado.
     *
     * @param token JWT completo (sem prefixo {@code Bearer})
     */
    public String parseUsername(String token) {
        Claims claims = Jwts.parser()
                .verifyWith(secretKey)
                .build()
                .parseSignedClaims(token)
                .getPayload();
        return claims.getSubject();
    }
}
