package com.artistas.api.domain;

import jakarta.persistence.*;
import lombok.*;

/**
 * Cópia local das regionais externas. Várias linhas por {@link #codigoExterno} ao longo do tempo
 * quando o nome muda: apenas uma com {@code ativo=true} por código.
 */
@Entity
@Table(name = "regional")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Regional {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /**
     * Identificador estável retornado pela API Argus (regra de negócio: sincronização por este código).
     */
    @Column(name = "codigo_externo", nullable = false)
    private Integer codigoExterno;

    @Column(nullable = false, length = 200)
    private String nome;

    @Column(nullable = false)
    private boolean ativo;
}
