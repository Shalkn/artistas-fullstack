package com.artistas.api.service;

import com.artistas.api.config.AppProperties;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;
import software.amazon.awssdk.services.s3.presigner.S3Presigner;
import software.amazon.awssdk.services.s3.presigner.model.GetObjectPresignRequest;

import java.io.IOException;
import java.time.Duration;
import java.util.UUID;

/**
 * Operações de objeto no bucket S3-compatível: upload de capas com chave namespaced por álbum
 * e geração de URL HTTP GET pré-assinada para leitura temporária (sem expor credenciais ao cliente).
 */
@Service
@RequiredArgsConstructor
public class StorageService {

    private final S3Client s3Client;
    private final S3Presigner s3Presigner;
    private final AppProperties appProperties;

    /**
     * Envia bytes do arquivo para {@code albums/{albumId}/uuid-nomeSeguro} e retorna a chave do objeto.
     * Caracteres não seguros no nome original são substituídos para evitar path traversal no key.
     *
     * @param albumId id do álbum (prefixo da chave)
     * @param file    multipart recebido do controller
     */
    public String uploadAlbumCover(Long albumId, MultipartFile file) throws IOException {
        String safeName = file.getOriginalFilename() != null ? file.getOriginalFilename() : "cover";
        String key = "albums/" + albumId + "/" + UUID.randomUUID() + "-" + safeName.replaceAll("[^a-zA-Z0-9._-]", "_");
        String bucket = appProperties.getStorage().getBucket();
        PutObjectRequest put = PutObjectRequest.builder()
                .bucket(bucket)
                .key(key)
                .contentType(file.getContentType() != null ? file.getContentType() : "application/octet-stream")
                .build();
        s3Client.putObject(put, RequestBody.fromBytes(file.getBytes()));
        return key;
    }

    /**
     * Gera URL assinada para download direto do objeto, válida por {@code app.storage.presign-get-minutes}.
     *
     * @param objectKey chave retornada por {@link #uploadAlbumCover(Long, MultipartFile)}
     */
    public String presignGet(String objectKey) {
        int minutes = appProperties.getStorage().getPresignGetMinutes();
        var getRequest = software.amazon.awssdk.services.s3.model.GetObjectRequest.builder()
                .bucket(appProperties.getStorage().getBucket())
                .key(objectKey)
                .build();
        GetObjectPresignRequest presignRequest = GetObjectPresignRequest.builder()
                .signatureDuration(Duration.ofMinutes(minutes))
                .getObjectRequest(getRequest)
                .build();
        return s3Presigner.presignGetObject(presignRequest).url().toExternalForm();
    }
}
