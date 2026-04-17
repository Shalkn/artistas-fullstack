package com.artistas.api.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;

import java.util.Arrays;
import java.util.List;

/**
 * Propriedades prefixadas por {@code app.*} (YAML / variáveis de ambiente). Agrupa JWT, armazenamento,
 * URL da API Argus e rate limit. A subclasse {@link Cors} está disponível para uso futuro; o
 * {@link com.artistas.api.config.SecurityConfig} atual monta CORS via padrão fixo no código.
 */
@Getter
@Setter
@ConfigurationProperties(prefix = "app")
public class AppProperties {

    private final Cors cors = new Cors();
    private final Jwt jwt = new Jwt();
    private final Storage storage = new Storage();
    private final Argus argus = new Argus();
    private final RateLimit rateLimit = new RateLimit();

    @Getter
    @Setter
    public static class Cors {
        /**
         * Origens permitidas (lista exata), separadas por vírgula.
         * Inclui {@code http://localhost} com e sem porta explícita — o padrão {@code http://localhost:*}
         * do Spring não casa com {@code Origin: http://localhost} em alguns casos.
         */
        private String allowedOrigins =
                "http://localhost,http://127.0.0.1,http://localhost:80,http://127.0.0.1:80,"
                        + "http://localhost:5173,http://127.0.0.1:5173,http://localhost:8080,http://127.0.0.1:8080";

        /** Lista parseada de {@link #allowedOrigins} (vírgulas). */
        public List<String> allowedOriginList() {
            return Arrays.stream(allowedOrigins.split(","))
                    .map(String::trim)
                    .filter(s -> !s.isEmpty())
                    .toList();
        }
    }

    @Getter
    @Setter
    public static class Jwt {
        private String secret = "change-me";
        private int accessTokenMinutes = 5;
        private int refreshTokenDays = 7;
    }

    @Getter
    @Setter
    public static class Storage {
        private String bucket = "artistas-covers";
        private String region = "us-east-1";
        private String endpoint = "";
        private String accessKey = "";
        private String secretKey = "";
        private int presignGetMinutes = 30;
    }

    @Getter
    @Setter
    public static class Argus {
        private String regionaisUrl = "https://integrador-argus-api.geia.vip/v1/regionais";
    }

    @Getter
    @Setter
    public static class RateLimit {
        /** Quando false, o filtro não aplica limite (útil em perfil local). */
        private boolean enabled = true;
        private int requestsPerMinute = 10;
    }
}
