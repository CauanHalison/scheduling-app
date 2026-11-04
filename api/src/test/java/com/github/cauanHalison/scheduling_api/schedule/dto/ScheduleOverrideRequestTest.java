package com.github.cauanHalison.scheduling_api.schedule.dto;

import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Set;
import java.util.stream.Collectors;

import static org.assertj.core.api.Assertions.assertThat;

class ScheduleOverrideRequestTest {

    private static Validator validator;

    @BeforeAll
    static void setUp() {
        validator = Validation.buildDefaultValidatorFactory().getValidator();
    }

    @Test
    @DisplayName("Should accept a valid schedule override")
    void shouldAcceptValidScheduleOverride() {
        ScheduleOverrideRequest request = new ScheduleOverrideRequest(
                daysFromNow(1),
                daysFromNow(2),
                "Reason test"
        );

        assertThat(validator.validate(request)).isEmpty();
    }

    @Test
    @DisplayName("Should accept schedule override without reason")
    void shouldAcceptScheduleOverrideWithoutReason() {
        ScheduleOverrideRequest request = new ScheduleOverrideRequest(
                daysFromNow(1),
                daysFromNow(2),
                null
        );

        assertThat(validator.validate(request)).isEmpty();
    }

    @Test
    @DisplayName("Should accept ongoing schedule override starting in the past and ending in the future")
    void shouldAcceptOngoingScheduleOverride() {
        ScheduleOverrideRequest request = new ScheduleOverrideRequest(
                daysFromNow(-1),
                daysFromNow(1),
                "Ongoing block"
        );

        assertThat(validator.validate(request)).isEmpty();
    }

    @Test
    @DisplayName("Should reject null start time or null end time")
    void shouldRejectNullStartOrEndTime() {
        ScheduleOverrideRequest nullEndTimeRequest = new ScheduleOverrideRequest(
                daysFromNow(1),
                null,
                null
        );
        assertThat(messages(nullEndTimeRequest)).contains("A data de fim é obrigatória.");

        ScheduleOverrideRequest nullStartTimeRequest = new ScheduleOverrideRequest(
                null,
                daysFromNow(1),
                null
        );
        assertThat(messages(nullStartTimeRequest)).contains("A data de ínicio é obrigatória.");
    }

    @Test
    @DisplayName("Should reject start time after end time")
    void shouldRejectStartTimeAfterEndTime() {
        ScheduleOverrideRequest request = new ScheduleOverrideRequest(
                daysFromNow(2),
                daysFromNow(1),
                null
        );

        assertThat(messages(request)).contains("A data final deve ser posterior à data inicial.");
    }

    @Test
    @DisplayName("Should reject start time equal to end time")
    void shouldRejectStartTimeEqualToEndTime() {
        Instant future = daysFromNow(1);

        ScheduleOverrideRequest request = new ScheduleOverrideRequest(
                future,
                future,
                null
        );

        assertThat(messages(request)).contains("A data final deve ser posterior à data inicial.");
    }

    @Test
    @DisplayName("Should reject end time in the past")
    void shouldRejectEndTimeInPast() {
        ScheduleOverrideRequest request = new ScheduleOverrideRequest(
                daysFromNow(-2),
                daysFromNow(-1),
                null
        );

        assertThat(messages(request)).contains("A data final deve ser no futuro.");
    }

    @Test
    @DisplayName("Should reject reason longer than 255 characters")
    void shouldRejectReasonLongerThan255Characters() {
        ScheduleOverrideRequest validMaxReason = new ScheduleOverrideRequest(
                daysFromNow(1),
                daysFromNow(2),
                "a".repeat(255)
        );
        assertThat(validator.validate(validMaxReason)).isEmpty();

        ScheduleOverrideRequest tooLongReason = new ScheduleOverrideRequest(
                daysFromNow(1),
                daysFromNow(2),
                "a".repeat(256)
        );
        assertThat(messages(tooLongReason)).contains("A justificativa pode ter no máximo 255 caracteres.");
    }

    private static Instant daysFromNow(long days) {
        return Instant.now().truncatedTo(ChronoUnit.SECONDS).plus(days, ChronoUnit.DAYS);
    }

    private static Set<String> messages(ScheduleOverrideRequest request) {
        return validator.validate(request).stream()
                .map(ConstraintViolation::getMessage)
                .collect(Collectors.toSet());
    }

}
