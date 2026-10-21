package com.github.cauanHalison.scheduling_api.exception.dto;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Representa um campo específico com erro de validação")
public record ErrorField(
        @Schema(description = "Nome do campo que gerou o erro", example = "email")
        String field,

        @Schema(description = "Descrição detalhada do erro encontrado", example = "deve ser um endereço de e-mail bem formado")
        String detail
) {
}
