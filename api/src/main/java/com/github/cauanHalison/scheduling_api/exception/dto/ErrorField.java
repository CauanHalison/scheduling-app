package com.github.cauanHalison.scheduling_api.exception.dto;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Represents a specific field with a validation error")
public record ErrorField(
        @Schema(description = "Name of the field that caused the error", example = "email")
        String field,

        @Schema(description = "Detailed description of the validation error", example = "Formato de email inválido")
        String detail
) {
}
