package com.artistas.api;

import com.artistas.api.config.AppProperties;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;

/**
 * Ponto de entrada da API REST (Spring Boot). Habilita o binding tipado de {@link AppProperties}
 * para JWT, armazenamento S3/MinIO, Argus e rate limit.
 */
@SpringBootApplication
@EnableConfigurationProperties(AppProperties.class)
public class ArtistasApiApplication {

    /**
     * Inicia o contexto Spring e sobe o servidor embutido (porta em {@code server.port} / {@code APP_PORT}).
     *
     * @param args argumentos de linha de comando (ex.: {@code --spring.profiles.active=local})
     */
    public static void main(String[] args) {
        SpringApplication.run(ArtistasApiApplication.class, args);
    }
}
