package com.artistas.api.config;

import io.github.bucket4j.Bandwidth;
import io.github.bucket4j.Bucket;
import io.github.bucket4j.Refill;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.lang.NonNull;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.time.Duration;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Limite configurável por minuto por usuário autenticado (chave {@code user:login}) ou por IP nas rotas
 * públicas ({@code ip:...}). Aplica-se apenas a URIs sob {@code /api/}; pode ser desligado via
 * {@code app.rate-limit.enabled} (ex.: perfil {@code local}).
 */
@RequiredArgsConstructor
public class RateLimitingFilter extends OncePerRequestFilter {

    private final AppProperties appProperties;
    private final Map<String, Bucket> cache = new ConcurrentHashMap<>();

    /**
     * Consome um token do bucket do cliente; se esgotado, responde 429 JSON sem chamar o controller.
     */
    @Override
    protected void doFilterInternal(
            @NonNull HttpServletRequest request,
            @NonNull HttpServletResponse response,
            @NonNull FilterChain filterChain) throws ServletException, IOException {

        if (!request.getRequestURI().startsWith("/api/")) {
            filterChain.doFilter(request, response);
            return;
        }

        if (!appProperties.getRateLimit().isEnabled()) {
            filterChain.doFilter(request, response);
            return;
        }

        int max = appProperties.getRateLimit().getRequestsPerMinute();
        String key = resolveKey(request);
        Bucket bucket = cache.computeIfAbsent(key, k -> buildBucket(max));
        if (!bucket.tryConsume(1)) {
            response.setStatus(HttpStatus.TOO_MANY_REQUESTS.value());
            response.setContentType("application/json;charset=UTF-8");
            response.getWriter().write("{\"message\":\"Limite de requisições excedido\"}");
            return;
        }
        filterChain.doFilter(request, response);
    }

    /** Bucket “clássico” com recarga greedy a cada minuto (janela fixa de 1 minuto). */
    private static Bucket buildBucket(int maxPerMinute) {
        Bandwidth limit = Bandwidth.classic(maxPerMinute, Refill.greedy(maxPerMinute, Duration.ofMinutes(1)));
        return Bucket.builder().addLimit(limit).build();
    }

    /**
     * Prioriza o nome do usuário autenticado; caso contrário usa o primeiro IP de {@code X-Forwarded-For}
     * ou {@code remoteAddr}.
     */
    private String resolveKey(HttpServletRequest request) {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth != null && auth.isAuthenticated() && auth.getName() != null) {
            return "user:" + auth.getName();
        }
        String forwarded = request.getHeader("X-Forwarded-For");
        if (forwarded != null && !forwarded.isBlank()) {
            return "ip:" + forwarded.split(",")[0].trim();
        }
        return "ip:" + request.getRemoteAddr();
    }
}
