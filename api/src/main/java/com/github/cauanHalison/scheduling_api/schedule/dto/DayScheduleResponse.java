package com.github.cauanHalison.scheduling_api.schedule.dto;

import java.time.DayOfWeek;
import java.util.List;

public record DayScheduleResponse(
        DayOfWeek dayOfWeek,
        List<TimeIntervalResponse> intervals
) {
}
