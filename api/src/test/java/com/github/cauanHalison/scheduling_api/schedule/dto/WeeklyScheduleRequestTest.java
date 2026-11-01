package com.github.cauanHalison.scheduling_api.schedule.dto;

import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import java.time.DayOfWeek;
import java.time.LocalTime;
import java.util.Arrays;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import static java.time.DayOfWeek.*;
import static org.assertj.core.api.AssertionsForInterfaceTypes.assertThat;

class WeeklyScheduleRequestTest {

    private static Validator validator;

    @BeforeAll
    static void setUpValidator() {
        validator = Validation.buildDefaultValidatorFactory().getValidator();
    }

    @Test
    @DisplayName("Should accept a valid weekly schedule")
    void shouldAcceptValidWeeklySchedule() {
        WeeklyScheduleRequest request = week(
                day(MONDAY, interval("08:00", "12:00"), interval("13:00", "17:00")),
                day(TUESDAY)
        );

        assertThat(validator.validate(request)).isEmpty();
    }

    @Test
    @DisplayName("Should accept empty days")
    void shouldAcceptEmptyDays() {
        assertThat(validator.validate(week())).isEmpty();
    }

    @Test
    @DisplayName("Should reject null days")
    void shouldRejectNullDays() {
        assertThat(messages(new WeeklyScheduleRequest(null)))
                .contains("A lista de dias é obrigatória");
    }

    @Test
    @DisplayName("Should reject duplicated day")
    void shouldRejectDuplicatedDay() {
        WeeklyScheduleRequest request = week(
                day(MONDAY, interval("08:00", "12:00"), interval("13:00", "17:00")),
                day(MONDAY, interval("08:00", "12:00"), interval("13:00", "17:00"))
        );

        assertThat(messages(request)).contains("Cada dia da semana só pode aparecer uma vez");
    }

    @Test
    @DisplayName("Should reject more than seven days")
    void shouldRejectMoreThanSevenDays() {
        DayScheduleRequest[] days = new DayScheduleRequest[8];
        Arrays.fill(days, day(MONDAY));

        assertThat(messages(week(days))).contains("A grade semanal pode ter no máximo 7 dias");
    }

    @ParameterizedTest(name = "[{index}] {0}-{1} and {2}-{3} -> valid={4}")
    @CsvSource({
            "08:00, 12:00, 11:00, 14:00, false",
            "08:00, 18:00, 10:00, 11:00, false",
            "08:00, 12:00, 08:00, 12:00, false",
            "08:00, 12:00, 12:00, 14:00, true",
            "08:00, 12:00, 13:00, 17:00, true"
    })
    @DisplayName("Should validate overlap")
    void shouldValidateOverlap(String s1, String e1, String s2, String e2, boolean valid) {
        WeeklyScheduleRequest request = week(day(MONDAY, interval(s1, e1), interval(s2, e2)));

        assertThat(validator.validate(request).isEmpty()).isEqualTo(valid);
    }

    @Test
    @DisplayName("Should accept unsorted intervals")
    void shouldAcceptUnsortedIntervals() {
        WeeklyScheduleRequest request = week(
                day(MONDAY, interval("13:00", "17:00"),interval("08:00", "12:00"))
        );

        assertThat(validator.validate(request)).isEmpty();
    }

    @Test
    @DisplayName("Should accept same time range on different days")
    void shouldAcceptSameRangDifferentDays() {
        WeeklyScheduleRequest request = week(
                day(MONDAY, interval("08:00", "12:00")),
                day(TUESDAY, interval("08:00", "12:00"))
        );

        assertThat(validator.validate(request)).isEmpty();
    }

    @Test
    @DisplayName("Should reject more than 10 intervals in a day")
    void shouldRejectTooManyIntervals() {
        TimeIntervalRequest[] intervals = IntStream.range(0,11)
                .mapToObj(i -> interval(String.format("%02d:00", i), String.format("%02d:30", i)))
                .toArray(TimeIntervalRequest[]::new);

        assertThat(messages(week(day(MONDAY, intervals))))
                .contains("Cada dia pode ter no máximo 10 blocos de horário");
    }

    @ParameterizedTest
    @CsvSource({"12:00, 08:00", "08:00, 08:00"})
    @DisplayName("Should reject invalid period")
    void shouldRejectInvalidPeriod(String start, String end) {
        WeeklyScheduleRequest request = week(day(MONDAY, interval(start, end)));

        assertThat(messages(request)).contains("A hora de inicio deve ser anterior a hora de fim");
    }

    @Test
    @DisplayName("Should reject null fields without throwing")
    void shouldRejectNullFields() {
        DayScheduleRequest nullDay = new DayScheduleRequest(null, null);
        DayScheduleRequest nullElement = new DayScheduleRequest(
                TUESDAY, Arrays.asList(null, interval("08:00", "12:00")));
        DayScheduleRequest nullTimes = new DayScheduleRequest(
                WEDNESDAY, List.of(new TimeIntervalRequest(null, null)));

        assertThat(validator.validate(week(nullDay))).isNotEmpty();
        assertThat(validator.validate(week(nullElement))).isNotEmpty();
        assertThat(validator.validate(week(nullTimes))).isNotEmpty();
    }

    private static TimeIntervalRequest interval(String start, String end) {
        return new TimeIntervalRequest(LocalTime.parse(start), LocalTime.parse(end));
    }

    private static DayScheduleRequest day(DayOfWeek day, TimeIntervalRequest... intervals) {
        return new DayScheduleRequest(day, List.of(intervals));
    }

    private static WeeklyScheduleRequest week(DayScheduleRequest... days) {
        return new WeeklyScheduleRequest(List.of(days));
    }

    private static Set<String> messages(WeeklyScheduleRequest request) {
        return validator.validate(request).stream()
                .map(ConstraintViolation::getMessage)
                .collect(Collectors.toSet());
    }
}