package com.artistas.api.config;

import lombok.RequiredArgsConstructor;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.DefaultCredentialsProvider;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.CreateBucketRequest;
import software.amazon.awssdk.services.s3.model.HeadBucketRequest;
import software.amazon.awssdk.services.s3.presigner.S3Presigner;

import java.net.URI;

/**
 * Cliente AWS SDK v2 para S3 e presigner. Com {@code app.storage.endpoint} preenchido (MinIO/local),
 * usa credenciais estáticas e path-style; sem endpoint, delega a {@link DefaultCredentialsProvider}
 * (ex.: AWS real). Garante existência do bucket na inicialização.
 */
@Configuration
@RequiredArgsConstructor
public class S3Configuration {

    private final AppProperties appProperties;

    /**
     * Cliente síncrono para {@code PutObject}, {@code HeadBucket}, etc.
     */
    @Bean
    public S3Client s3Client() {
        AppProperties.Storage s = appProperties.getStorage();
        var builder = S3Client.builder()
                .region(Region.of(s.getRegion()));
        if (s.getEndpoint() != null && !s.getEndpoint().isBlank()) {
            builder.endpointOverride(URI.create(s.getEndpoint()))
                    .credentialsProvider(StaticCredentialsProvider.create(
                            AwsBasicCredentials.create(s.getAccessKey(), s.getSecretKey())))
                    .serviceConfiguration(software.amazon.awssdk.services.s3.S3Configuration.builder()
                            .pathStyleAccessEnabled(true)
                            .build());
        } else {
            builder.credentialsProvider(DefaultCredentialsProvider.create());
        }
        return builder.build();
    }

    /**
     * Usado exclusivamente para URLs GET pré-assinadas em {@link com.artistas.api.service.StorageService}.
     */
    @Bean
    public S3Presigner s3Presigner() {
        AppProperties.Storage s = appProperties.getStorage();
        var builder = S3Presigner.builder()
                .region(Region.of(s.getRegion()));
        if (s.getEndpoint() != null && !s.getEndpoint().isBlank()) {
            builder.endpointOverride(URI.create(s.getEndpoint()))
                    .credentialsProvider(StaticCredentialsProvider.create(
                            AwsBasicCredentials.create(s.getAccessKey(), s.getSecretKey())))
                    .serviceConfiguration(software.amazon.awssdk.services.s3.S3Configuration.builder()
                            .pathStyleAccessEnabled(true)
                            .build());
        } else {
            builder.credentialsProvider(DefaultCredentialsProvider.create());
        }
        return builder.build();
    }

    /**
     * Após o contexto subir, cria o bucket configurado se ainda não existir (idempotente).
     */
    @Bean
    ApplicationRunner ensureBucketRunner(S3Client s3Client) {
        return args -> {
            String bucket = appProperties.getStorage().getBucket();
            try {
                s3Client.headBucket(HeadBucketRequest.builder().bucket(bucket).build());
            } catch (Exception e) {
                s3Client.createBucket(CreateBucketRequest.builder().bucket(bucket).build());
            }
        };
    }
}
