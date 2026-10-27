package com.github.cauanHalison.scheduling_api.eventtype.dto;

import jakarta.validation.constraints.*;

public record EventTypeRequest(
        @NotBlank(message = "O título do evento é obrigatório")
        @Size(max = 100, message = "O título do evento deve ter no máximo 100 caracteres")
        String title,

        @NotBlank(message = "O slug do evento é obrigatório")
        @Size(min = 3, max = 100, message = "O slug do evento deve ter entre 3 e 100 caracteres")
        @Pattern(
                regexp = "^[a-z0-9]+(-[a-z0-9]+)*$",
                message = "O slug deve conter apenas letras minúsculas, números e hífens"
        )
        String slug,

        @Size(max = 2000, message = "A descrição deve ter no máximo 2000 caracteres")
        String description,

        @NotNull(message = "A duração do evento é obrigatória")
        @Min(value = 1, message = "O evento deve ter no mínimo 1 minuto de duração")
        @Max(value = 720, message = "O evento deve ter no máximo 720 minutos de duração")
        Integer durationMinutes,

        @NotNull(message = "A duração da pausa é obrigatória")
        @Min(value = 0, message = "O evento deve ter pausa de no mínimo 0 minutos de duração")
        @Max(value = 240, message = "O evento deve ter pausa de no máximo 240 minutos de duração")
        Integer bufferAfterMinutes
) {
}
