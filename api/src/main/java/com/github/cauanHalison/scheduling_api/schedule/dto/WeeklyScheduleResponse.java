package com.github.cauanHalison.scheduling_api.schedule.dto;

import java.util.List;

public record WeeklyScheduleResponse(
        String timeZone,
        List<DayScheduleResponse> days
) {
}
