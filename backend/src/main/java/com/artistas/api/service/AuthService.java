package com.artistas.api.service;

import com.artistas.api.config.AppProperties;
import com.artistas.api.domain.AppUser;
import com.artistas.api.domain.RefreshToken;
import com.artistas.api.dto.auth.LoginRequest;
import com.artistas.api.dto.auth.TokenResponse;
import com.artistas.api.repository.AppUserRepository;
import com.artistas.api.repository.RefreshTokenRepository;
import jakarta.persistence.EntityManager;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.security.SecureRandom;
import java.time.Instant;
import java.util.Base64;

/**
 * Autenticação por usuário/senha e rotação de refresh token. Regra de negócio: um único refresh
 * válido por usuário após novo login ({@code deleteByUserId}) e rotação em {@code refresh}
 * (token antigo invalidado antes de emitir par novo).
 */
@Service
@RequiredArgsConstructor
public class AuthService {

    private final EntityManager entityManager;
    private final AppUserRepository appUserRepository;
    private final RefreshTokenRepository refreshTokenRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtTokenService jwtTokenService;
    private final AppProperties appProperties;
    private final SecureRandom secureRandom = new SecureRandom();

    /**
     * Valida credenciais, remove refresh tokens anteriores do usuário e emite novo par access/refresh.
     *
     * @param request usuário e senha (validados no controller)
     * @throws BadCredentialsException se usuário inexistente, desabilitado ou senha incorreta
     */
    @Transactional
    public TokenResponse login(LoginRequest request) {
        AppUser user = appUserRepository.findByUsername(request.username())
                .orElseThrow(() -> new BadCredentialsException("Credenciais inválidas"));
        if (!user.isEnabled() || !passwordEncoder.matches(request.password(), user.getPasswordHash())) {
            throw new BadCredentialsException("Credenciais inválidas");
        }
        refreshTokenRepository.deleteByUserId(user.getId());
        return buildTokens(user);
    }

    /**
     * Troca um refresh token válido por um novo par. A linha do token é carregada com lock pessimista
     * ({@link com.artistas.api.repository.RefreshTokenRepository#findByToken}) para reduzir corrida.
     * <p>Se expirado, remove o token e responde erro; em caso de sucesso, remove o token usado antes
     * de criar outro (rotação).</p>
     *
     * @param refreshTokenRaw valor opaco armazenado no cliente
     */
    @Transactional
    public TokenResponse refresh(String refreshTokenRaw) {
        RefreshToken stored = refreshTokenRepository.findByToken(refreshTokenRaw)
                .orElseThrow(() -> new BadCredentialsException("Refresh token inválido"));
        if (stored.getExpiresAt().isBefore(Instant.now())) {
            // Evita que a entidade gerenciada entre em conflito com o DELETE em lote no flush.
            entityManager.detach(stored);
            refreshTokenRepository.deleteByToken(refreshTokenRaw);
            throw new BadCredentialsException("Refresh token expirado");
        }
        Long userId = stored.getUser().getId();
        entityManager.detach(stored);
        int removed = refreshTokenRepository.deleteByToken(refreshTokenRaw);
        if (removed == 0) {
            throw new BadCredentialsException("Refresh token inválido");
        }
        return buildTokens(appUserRepository.getReferenceById(userId));
    }

    /** Emite JWT de acesso e persiste novo refresh com expiração configurável ({@code app.jwt}). */
    private TokenResponse buildTokens(AppUser user) {
        String access = jwtTokenService.createAccessToken(user.getUsername());
        String refreshRaw = generateRefreshRaw();
        Instant refreshExp = Instant.now().plusSeconds(appProperties.getJwt().getRefreshTokenDays() * 86400L);
        refreshTokenRepository.save(RefreshToken.builder()
                .token(refreshRaw)
                .user(user)
                .expiresAt(refreshExp)
                .build());
        return new TokenResponse(access, refreshRaw, appProperties.getJwt().getAccessTokenMinutes() * 60);
    }

    /** Gera string opaca URL-safe (Base64) para o refresh token persistido. */
    private String generateRefreshRaw() {
        byte[] bytes = new byte[48];
        secureRandom.nextBytes(bytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }
}
