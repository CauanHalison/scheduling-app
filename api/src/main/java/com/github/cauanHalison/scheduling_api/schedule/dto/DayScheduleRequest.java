package com.github.cauanHalison.scheduling_api.schedule.dto;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.validation.Valid;
import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.time.DayOfWeek;
import java.util.Comparator;
import java.util.List;

public record DayScheduleRequest(
        @NotNull(message = "O dia da semana é obrigatório")
        DayOfWeek dayOfWeek,

        @NotNull(message = "A lista de horários do dia é obrigatória (use lista vazia para dia sem atendimento)")
        @Size(max = DayScheduleRequest.MAX_INTERVALS_PER_DAY,
                message = "Cada dia pode ter no máximo " + DayScheduleRequest.MAX_INTERVALS_PER_DAY + " blocos de horário")
        List<@NotNull(message = "Bloco de horário inválido") @Valid TimeIntervalRequest> intervals
) {
    public static final int MAX_INTERVALS_PER_DAY = 10;

    @JsonIgnore
    @AssertTrue(message = "Os blocos de horário do mesmo dia não podem se sobrepor")
    public boolean isIntervalsNotOverlapping() {
        if (intervals == null) {
            return true;
        }

        List<TimeIntervalRequest> sorted = intervals.stream()
                .filter(i -> i != null && i.startTime() != null && i.endTime() != null)
                .sorted(Comparator.comparing(TimeIntervalRequest::startTime))
                .toList();

        for (int i = 1; i < sorted.size(); i++) {
            if (sorted.get(i).startTime().isBefore(sorted.get(i - 1).endTime())) {
                return false;
            }
        }
        return true;
    }
}
