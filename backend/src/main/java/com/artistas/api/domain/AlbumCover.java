package com.artistas.api.domain;

import jakarta.persistence.*;
import lombok.*;

/**
 * Metadado de arquivo no bucket: a chave S3 não contém URL pública permanente; leitura via URL pré-assinada.
 */
@Entity
@Table(name = "album_cover")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AlbumCover {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "album_id", nullable = false)
    private Album album;

    @Column(name = "object_key", nullable = false, length = 1024)
    private String objectKey;

    @Column(name = "content_type", length = 120)
    private String contentType;

    @Column(name = "sort_order", nullable = false)
    private int sortOrder;
}
