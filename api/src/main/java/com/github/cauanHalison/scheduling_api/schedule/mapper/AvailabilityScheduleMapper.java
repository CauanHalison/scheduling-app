package com.github.cauanHalison.scheduling_api.schedule.mapper;

import com.github.cauanHalison.scheduling_api.identity.domain.ProviderEntity;
import com.github.cauanHalison.scheduling_api.schedule.domain.AvailabilityScheduleEntity;
import com.github.cauanHalison.scheduling_api.schedule.dto.*;
import org.springframework.stereotype.Component;

import java.time.DayOfWeek;
import java.util.*;

@Component
public class AvailabilityScheduleMapper {

    public List<AvailabilityScheduleEntity> toEntities(WeeklyScheduleRequest request, ProviderEntity provider) {
        List<AvailabilityScheduleEntity> entities = new ArrayList<>();

        for (DayScheduleRequest day : request.days()) {
            for (TimeIntervalRequest interval : day.intervals()) {
                AvailabilityScheduleEntity entity = new AvailabilityScheduleEntity();
                entity.setProvider(provider);
                entity.setDayOfWeek(day.dayOfWeek());
                entity.setStartTime(interval.startTime());
                entity.setEndTime(interval.endTime());
                entities.add(entity);
            }
        }

        return entities;
    }

    public WeeklyScheduleResponse toResponse(String timeZone, List<AvailabilityScheduleEntity> entities) {
        Map<DayOfWeek, List<TimeIntervalResponse>> intervalsByDay = new EnumMap<>(DayOfWeek.class);

        entities.stream()
                .sorted(Comparator.comparing(AvailabilityScheduleEntity::getStartTime))
                .forEach(entity -> intervalsByDay
                        .computeIfAbsent(entity.getDayOfWeek(), d -> new ArrayList<>())
                        .add(new TimeIntervalResponse(entity.getStartTime(), entity.getEndTime())));

        List<DayScheduleResponse> days = Arrays.stream(DayOfWeek.values())
                .map(day -> new DayScheduleResponse(day, intervalsByDay.getOrDefault(day, List.of())))
                .toList();

        return new WeeklyScheduleResponse(timeZone, days);
    }
}
