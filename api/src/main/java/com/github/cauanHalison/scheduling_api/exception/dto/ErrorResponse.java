package com.github.cauanHalison.scheduling_api.exception.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import io.swagger.v3.oas.annotations.media.Schema;

import java.time.Instant;
import java.util.List;

@Schema(description = "Estrutura padrão de resposta para erros da API")
@JsonInclude(JsonInclude.Include.NON_EMPTY)
public record ErrorResponse(
        @Schema(description = "Código de status HTTP do erro", example = "401")
        int statusCode,

        @Schema(description = "Rota", example = "ap1/vi/auth/login")
        String path,

        @Schema(description = "Mensagem descritiva do erro", example = "Autenticação necessária para acessar este recurso.")
        String message,

        @Schema(description = "Data e hora do erro em UTC (ISO-8601)", example = "2026-10-21T19:30:00Z")
        Instant timestamp,

        @Schema(description = "Lista de erros por campo (preenchido em falhas de validação)")
        List<ErrorField> errors
) {
    public ErrorResponse(int statusCode, String path, String message) {
        this(statusCode, path, message, List.of());
    }

    public ErrorResponse(int statusCode, String path, String message, List<ErrorField> errors) {
        this(statusCode, path, message, Instant.now(), errors != null ? errors : List.of());
    }
}
