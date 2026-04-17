package com.artistas.api.dto.regional;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Regional sincronizada da API Argus")
public record RegionalDto(
        Long id,
        Integer codigoExterno,
        String nome,
        boolean ativo
) {
}
