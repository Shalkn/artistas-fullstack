package com.artistas.api.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.reactive.function.client.WebClient;

/**
 * Expõe {@link WebClient.Builder} para integrações HTTP reativas (ex.: sincronização Argus em
 * {@link com.artistas.api.service.RegionalSyncService}).
 */
@Configuration
public class WebClientConfig {

    /**
     * Builder stateless; cada chamada pode {@code .build()} um cliente dedicado.
     */
    @Bean
    public WebClient.Builder webClientBuilder() {
        return WebClient.builder();
    }
}
