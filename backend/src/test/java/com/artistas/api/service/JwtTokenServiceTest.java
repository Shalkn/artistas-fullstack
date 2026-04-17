package com.artistas.api.service;

import com.artistas.api.config.AppProperties;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/** Testes unitários do JWT sem contexto Spring (segredo mínimo de 32 caracteres). */
class JwtTokenServiceTest {

    private JwtTokenService jwtTokenService;

    @BeforeEach
    void setUp() {
        AppProperties props = new AppProperties();
        props.getJwt().setSecret("test-secret-key-at-least-32-chars-long!!");
        props.getJwt().setAccessTokenMinutes(5);
        jwtTokenService = new JwtTokenService(props);
    }

    @Test
    void deveGerarEValidarTokenComMesmoSubject() {
        String token = jwtTokenService.createAccessToken("admin");
        assertThat(jwtTokenService.parseUsername(token)).isEqualTo("admin");
    }
}
