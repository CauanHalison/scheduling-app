package com.github.cauanHalison.scheduling_api.schedule.mapper;

import com.github.cauanHalison.scheduling_api.identity.domain.ProviderEntity;
import com.github.cauanHalison.scheduling_api.schedule.domain.AvailabilityScheduleEntity;
import com.github.cauanHalison.scheduling_api.schedule.dto.*;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.DayOfWeek;
import java.time.LocalTime;
import java.util.List;
import java.util.UUID;

import static java.time.DayOfWeek.*;
import static org.assertj.core.api.AssertionsForClassTypes.tuple;
import static org.assertj.core.api.AssertionsForInterfaceTypes.assertThat;

class AvailabilityScheduleMapperTest {
    private final AvailabilityScheduleMapper mapper = new AvailabilityScheduleMapper();

    @Test
    @DisplayName("Should flatten days and intervals into entities linked to the provider")
    void shouldMapRequestToEntities() {
        ProviderEntity provider = new ProviderEntity();
        provider.setId(UUID.randomUUID());

        WeeklyScheduleRequest request = new WeeklyScheduleRequest(List.of(
                new DayScheduleRequest(MONDAY, List.of(
                        new TimeIntervalRequest(LocalTime.of(8, 0), LocalTime.of(12, 0)),
                        new TimeIntervalRequest(LocalTime.of(13, 0), LocalTime.of(17, 0)))),
                new DayScheduleRequest(TUESDAY, List.of(
                        new TimeIntervalRequest(LocalTime.of(9, 0), LocalTime.of(11, 0))
                ))
        ));

        List<AvailabilityScheduleEntity> entities = mapper.toEntities(request, provider);

        assertThat(entities).hasSize(3);
        assertThat(entities).allSatisfy(e -> assertThat(e.getProvider()).isSameAs(provider));
        assertThat(entities)
                .extracting(AvailabilityScheduleEntity::getDayOfWeek,
                        AvailabilityScheduleEntity::getStartTime,
                        AvailabilityScheduleEntity::getEndTime)
                .containsExactly(
                        tuple(MONDAY, LocalTime.of(8, 0), LocalTime.of(12, 0)),
                        tuple(MONDAY, LocalTime.of(13, 0), LocalTime.of(17, 0)),
                        tuple(TUESDAY, LocalTime.of(9, 0), LocalTime.of(11, 0)));
    }

    @Test
    @DisplayName("Should return no entities for days without intervals")
    void shouldReturnNoEntitiesForEmptyDays() {
        WeeklyScheduleRequest request = new WeeklyScheduleRequest(List.of(
                new DayScheduleRequest(MONDAY, List.of())));

        assertThat(mapper.toEntities(request, new ProviderEntity())).isEmpty();
    }

    @Test
    @DisplayName("Should always return the 7 days, Monday to Sunday, even with no entities")
    void shouldReturnSevenEmptyDays() {
        WeeklyScheduleResponse response = mapper.toResponse("America/Sao_Paulo", List.of());

        assertThat(response.timeZone()).isEqualTo("America/Sao_Paulo");
        assertThat(response.days())
                .extracting(DayScheduleResponse::dayOfWeek)
                .containsExactly(MONDAY, TUESDAY, WEDNESDAY, THURSDAY, FRIDAY, SATURDAY, SUNDAY);
        assertThat(response.days()).allSatisfy(d -> assertThat(d.intervals()).isEmpty());
    }

    @Test
    @DisplayName("Should group by day and sort intervals by start time")
    void shouldGroupByDayAndSortIntervals() {
        List<AvailabilityScheduleEntity> entities = List.of(
                entity(WEDNESDAY, "14:00", "18:00"),
                entity(MONDAY, "13:00", "17:00"),
                entity(MONDAY, "08:00", "12:00"));

        WeeklyScheduleResponse response = mapper.toResponse("UTC", entities);

        DayScheduleResponse monday = response.days().getFirst();
        assertThat(monday.dayOfWeek()).isEqualTo(MONDAY);
        assertThat(monday.intervals()).containsExactly(
                new TimeIntervalResponse(LocalTime.of(8, 0), LocalTime.of(12, 0)),
                new TimeIntervalResponse(LocalTime.of(13, 0), LocalTime.of(17, 0)));

        assertThat(response.days().get(1).intervals()).isEmpty();   // Tuesday
        assertThat(response.days().get(2).intervals())              // Wednesday
                .containsExactly(new TimeIntervalResponse(LocalTime.of(14, 0), LocalTime.of(18, 0)));
    }

    private static AvailabilityScheduleEntity entity(DayOfWeek day, String start, String end) {
        AvailabilityScheduleEntity e = new AvailabilityScheduleEntity();
        e.setDayOfWeek(day);
        e.setStartTime(LocalTime.parse(start));
        e.setEndTime(LocalTime.parse(end));
        return e;
    }
}