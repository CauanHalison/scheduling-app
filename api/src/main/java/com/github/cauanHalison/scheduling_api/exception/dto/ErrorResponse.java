package com.github.cauanHalison.scheduling_api.exception.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import io.swagger.v3.oas.annotations.media.Schema;

import java.time.Instant;
import java.util.List;

@Schema(description = "Standard API error response structure")
@JsonInclude(JsonInclude.Include.NON_EMPTY)
public record ErrorResponse(
        @Schema(description = "HTTP status code of the error", example = "401")
        int statusCode,

        @Schema(description = "Request path", example = "/api/v1/auth/login")
        String path,

        @Schema(description = "Descriptive error message", example = "Autenticação necessária para acessar este recurso.")
        String message,

        @Schema(description = "Error timestamp in UTC (ISO-8601)", example = "2026-10-21T19:30:00Z")
        Instant timestamp,

        @Schema(description = "List of field validation errors (populated on validation failures)")
        List<ErrorField> errors
) {
    public ErrorResponse(int statusCode, String path, String message) {
        this(statusCode, path, message, List.of());
    }

    public ErrorResponse(int statusCode, String path, String message, List<ErrorField> errors) {
        this(statusCode, path, message, Instant.now(), errors != null ? errors : List.of());
    }
}
