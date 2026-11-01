package com.github.cauanHalison.scheduling_api.schedule.dto;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.validation.Valid;
import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.time.DayOfWeek;
import java.util.List;
import java.util.Objects;

public record WeeklyScheduleRequest(
        @NotNull(message = "A lista de dias é obrigatória")
        @Size(max = 7, message = "A grade semanal pode ter no máximo 7 dias")
        List<@NotNull(message = "Dia inválido") @Valid DayScheduleRequest> days
) {
    @JsonIgnore
    @AssertTrue(message = "Cada dia da semana só pode aparecer uma vez")
    public boolean isDaysUnique() {
        if (days == null) {
            return true;
        }

        List<DayOfWeek> informed = days.stream()
                .filter(Objects::nonNull)
                .map(DayScheduleRequest::dayOfWeek)
                .filter(Objects::nonNull)
                .toList();

        return informed.size() == informed.stream().distinct().count();
    }
}
