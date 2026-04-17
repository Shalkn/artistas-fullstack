package com.artistas.api.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import org.springframework.data.domain.Page;

import java.util.List;

/**
 * Envelope genérico alinhado ao {@link org.springframework.data.domain.Page} do Spring Data.
 *
 * @param <T> tipo do item em {@link #content}
 */
@Schema(description = "Resposta paginada")
public record PageResponse<T>(
        List<T> content,
        int page,
        int size,
        long totalElements,
        int totalPages,
        boolean last
) {
    /**
     * Converte uma página Spring Data no formato exposto pela API.
     *
     * @param page resultado de repositório já paginado
     * @param <T>  tipo do conteúdo
     */
    public static <T> PageResponse<T> from(Page<T> page) {
        return new PageResponse<>(
                page.getContent(),
                page.getNumber(),
                page.getSize(),
                page.getTotalElements(),
                page.getTotalPages(),
                page.isLast()
        );
    }
}
