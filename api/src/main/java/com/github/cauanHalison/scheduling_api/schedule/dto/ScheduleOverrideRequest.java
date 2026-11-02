package com.github.cauanHalison.scheduling_api.schedule.dto;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.validation.constraints.*;

import java.time.Instant;

public record ScheduleOverrideRequest(
        @NotNull(message = "A data de ínicio é obrigatória.")
        Instant startTime,

        @NotNull(message = "A data de fim é obrigatória.")
        @Future(message = "A data final deve ser no futuro.")
        Instant endTime,

        @Size(max = 255, message = "A justificativa pode ter no máximo 255 caracteres.")
        String reason
) {
    @JsonIgnore
    @AssertTrue(message = "A data final deve ser posterior à data inicial.")
    public boolean isTimeRangeValid() {
        return startTime == null || endTime == null || endTime.isAfter(startTime);
    };
}
