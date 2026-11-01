package com.github.cauanHalison.scheduling_api.schedule.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.NotNull;

import java.time.LocalTime;

public record TimeIntervalRequest(
        @NotNull(message = "A hora de inicio é obrigatória")
        @JsonFormat(pattern = "HH:mm")
        LocalTime startTime,

        @NotNull(message = "A hora de fim é obrigatória")
        @JsonFormat(pattern = "HH:mm")
        LocalTime endTime
) {
    @JsonIgnore
    @AssertTrue(message = "A hora de inicio deve ser anterior a hora de fim")
    public boolean isPeriodValid() {
        return startTime == null || endTime == null || startTime.isBefore(endTime);
    }
}
