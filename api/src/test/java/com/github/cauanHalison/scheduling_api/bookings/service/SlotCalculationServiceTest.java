package com.github.cauanHalison.scheduling_api.bookings.service;

import com.github.cauanHalison.scheduling_api.bookings.domain.BookingEntity;
import com.github.cauanHalison.scheduling_api.bookings.domain.enums.BookingStatus;
import com.github.cauanHalison.scheduling_api.bookings.dto.AvailableDaysResponse;
import com.github.cauanHalison.scheduling_api.bookings.dto.AvailableSlotsResponse;
import com.github.cauanHalison.scheduling_api.bookings.repository.BookingRepository;
import com.github.cauanHalison.scheduling_api.eventtype.domain.EventTypeEntity;
import com.github.cauanHalison.scheduling_api.eventtype.repository.EventTypeRepository;
import com.github.cauanHalison.scheduling_api.exception.exceptions.ResourceNotFoundException;
import com.github.cauanHalison.scheduling_api.identity.domain.ProviderEntity;
import com.github.cauanHalison.scheduling_api.schedule.domain.AvailabilityScheduleEntity;
import com.github.cauanHalison.scheduling_api.schedule.domain.ScheduleOverrideEntity;
import com.github.cauanHalison.scheduling_api.schedule.repository.AvailabilityScheduleRepository;
import com.github.cauanHalison.scheduling_api.schedule.repository.ScheduleOverrideRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.*;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class SlotCalculationServiceTest {

    @Mock
    private EventTypeRepository eventTypeRepository;

    @Mock
    private AvailabilityScheduleRepository availabilityScheduleRepository;

    @Mock
    private ScheduleOverrideRepository scheduleOverrideRepository;

    @Mock
    private BookingRepository bookingRepository;

    @InjectMocks
    private SlotCalculationService slotCalculationService;

    private ProviderEntity provider;
    private EventTypeEntity eventType;
    private final ZoneId providerZone = ZoneId.of("America/Sao_Paulo");

    @BeforeEach
    void setUp() {
        provider = new ProviderEntity();
        provider.setId(UUID.randomUUID());
        provider.setName("Provider");
        provider.setSlug("provider-slug");
        provider.setTimeZone(providerZone.getId());

        eventType = new EventTypeEntity();
        eventType.setId(UUID.randomUUID());
        eventType.setProvider(provider);
        eventType.setTitle("Consultoria 30min");
        eventType.setSlug("consultoria-30min");
        eventType.setDurationMinutes(30);
        eventType.setBufferAfterMinutes(10);
        eventType.setIsActive(true);
    }

    @Test
    @DisplayName("Should return available slots sliced according to duration and shift")
    void shouldReturnAvailableSlotsAccordingToDuration() {
        LocalDate futureMonday = getNextFutureMonday();

        AvailabilityScheduleEntity mondaySchedule = new AvailabilityScheduleEntity();
        mondaySchedule.setId(UUID.randomUUID());
        mondaySchedule.setProvider(provider);
        mondaySchedule.setDayOfWeek(DayOfWeek.MONDAY);
        mondaySchedule.setStartTime(LocalTime.of(9, 0));
        mondaySchedule.setEndTime(LocalTime.of(10, 0)); 

        when(eventTypeRepository.findByProviderSlugAndSlugAndIsActiveTrue("provider-slug", "consultoria-30min"))
                .thenReturn(Optional.of(eventType));
        when(availabilityScheduleRepository.findAllByProviderId(provider.getId())).thenReturn(List.of(mondaySchedule));
        when(scheduleOverrideRepository.findOverlapping(eq(provider.getId()), any(Instant.class), any(Instant.class))).thenReturn(List.of());
        when(bookingRepository.findConfirmedOverlapping(eq(provider.getId()), any(Instant.class), any(Instant.class))).thenReturn(List.of());

        AvailableSlotsResponse response = slotCalculationService.getAvailableSlots("provider-slug", "consultoria-30min", futureMonday, providerZone);

        assertThat(response).isNotNull();
        assertThat(response.date()).isEqualTo(futureMonday);
        assertThat(response.slots()).hasSize(2);

        Instant expectedFirstSlot = futureMonday.atTime(9, 0).atZone(providerZone).toInstant();
        Instant expectedSecondSlot = futureMonday.atTime(9, 30).atZone(providerZone).toInstant();

        assertThat(response.slots().get(0).startTime()).isEqualTo(expectedFirstSlot);
        assertThat(response.slots().get(1).startTime()).isEqualTo(expectedSecondSlot);
    }

    @Test
    @DisplayName("Should subtract schedule overrides from available slots")
    void shouldSubtractScheduleOverridesFromAvailableSlots() {
        LocalDate futureMonday = getNextFutureMonday();

        AvailabilityScheduleEntity mondaySchedule = new AvailabilityScheduleEntity();
        mondaySchedule.setProvider(provider);
        mondaySchedule.setDayOfWeek(DayOfWeek.MONDAY);
        mondaySchedule.setStartTime(LocalTime.of(9, 0));
        mondaySchedule.setEndTime(LocalTime.of(12, 0)); 

        ScheduleOverrideEntity override = new ScheduleOverrideEntity();
        override.setProvider(provider);
        override.setStartTime(futureMonday.atTime(10, 0).atZone(providerZone).toInstant());
        override.setEndTime(futureMonday.atTime(11, 0).atZone(providerZone).toInstant());

        when(eventTypeRepository.findByProviderSlugAndSlugAndIsActiveTrue("provider-slug", "consultoria-30min"))
                .thenReturn(Optional.of(eventType));
        when(availabilityScheduleRepository.findAllByProviderId(provider.getId())).thenReturn(List.of(mondaySchedule));
        when(scheduleOverrideRepository.findOverlapping(eq(provider.getId()), any(Instant.class), any(Instant.class))).thenReturn(List.of(override));
        when(bookingRepository.findConfirmedOverlapping(eq(provider.getId()), any(Instant.class), any(Instant.class))).thenReturn(List.of());

        AvailableSlotsResponse response = slotCalculationService.getAvailableSlots("provider-slug", "consultoria-30min", futureMonday, providerZone);

        assertThat(response.slots()).hasSize(4);

        Instant tenAm = futureMonday.atTime(10, 0).atZone(providerZone).toInstant();
        assertThat(response.slots()).noneMatch(s -> s.startTime().equals(tenAm));
    }

    @Test
    @DisplayName("Should respect buffer_after by subtracting until blockedUntil of existing booking")
    void shouldRespectBufferAfterOfExistingBooking() {
        LocalDate futureMonday = getNextFutureMonday();

        AvailabilityScheduleEntity mondaySchedule = new AvailabilityScheduleEntity();
        mondaySchedule.setProvider(provider);
        mondaySchedule.setDayOfWeek(DayOfWeek.MONDAY);
        mondaySchedule.setStartTime(LocalTime.of(9, 0));
        mondaySchedule.setEndTime(LocalTime.of(10, 30));

        BookingEntity existingBooking = new BookingEntity();
        existingBooking.setStartTime(futureMonday.atTime(9, 0).atZone(providerZone).toInstant());
        existingBooking.setEndTime(futureMonday.atTime(9, 30).atZone(providerZone).toInstant());
        existingBooking.setBlockedUntil(futureMonday.atTime(9, 40).atZone(providerZone).toInstant());
        existingBooking.setStatus(BookingStatus.CONFIRMED);

        when(eventTypeRepository.findByProviderSlugAndSlugAndIsActiveTrue("provider-slug", "consultoria-30min"))
                .thenReturn(Optional.of(eventType));
        when(availabilityScheduleRepository.findAllByProviderId(provider.getId())).thenReturn(List.of(mondaySchedule));
        when(scheduleOverrideRepository.findOverlapping(eq(provider.getId()), any(Instant.class), any(Instant.class))).thenReturn(List.of());
        when(bookingRepository.findConfirmedOverlapping(eq(provider.getId()), any(Instant.class), any(Instant.class))).thenReturn(List.of(existingBooking));

        AvailableSlotsResponse response = slotCalculationService.getAvailableSlots("provider-slug", "consultoria-30min", futureMonday, providerZone);

        assertThat(response.slots()).hasSize(1);
        Instant expectedSlot = futureMonday.atTime(9, 40).atZone(providerZone).toInstant();
        assertThat(response.slots().getFirst().startTime()).isEqualTo(expectedSlot);
    }

    @Test
    @DisplayName("Should throw ResourceNotFoundException when event slug does not exist")
    void shouldThrowWhenEventSlugNotFound() {
        when(eventTypeRepository.findByProviderSlugAndSlugAndIsActiveTrue("provider-slug", "invalid-slug"))
                .thenReturn(Optional.empty());

        assertThatThrownBy(() -> slotCalculationService.getAvailableSlots("provider-slug", "invalid-slug", LocalDate.now(), providerZone))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessage("Tipo de evento não encontrado para este prestador ou inativo.");
    }

    @Test
    @DisplayName("Should return available days for future month")
    void shouldReturnAvailableDaysForMonth() {
        LocalDate futureMonday = getNextFutureMonday();
        YearMonth targetMonth = YearMonth.from(futureMonday);

        AvailabilityScheduleEntity mondaySchedule = new AvailabilityScheduleEntity();
        mondaySchedule.setProvider(provider);
        mondaySchedule.setDayOfWeek(DayOfWeek.MONDAY);
        mondaySchedule.setStartTime(LocalTime.of(9, 0));
        mondaySchedule.setEndTime(LocalTime.of(10, 0));

        when(eventTypeRepository.findByProviderSlugAndSlugAndIsActiveTrue("provider-slug", "consultoria-30min"))
                .thenReturn(Optional.of(eventType));
        when(availabilityScheduleRepository.findAllByProviderId(provider.getId())).thenReturn(List.of(mondaySchedule));
        when(scheduleOverrideRepository.findOverlapping(eq(provider.getId()), any(Instant.class), any(Instant.class))).thenReturn(List.of());
        when(bookingRepository.findConfirmedOverlapping(eq(provider.getId()), any(Instant.class), any(Instant.class))).thenReturn(List.of());

        AvailableDaysResponse response = slotCalculationService.getAvailableDays("provider-slug", "consultoria-30min", targetMonth, providerZone);

        assertThat(response.availableDays()).contains(futureMonday);
    }

    private LocalDate getNextFutureMonday() {
        LocalDate date = LocalDate.now(providerZone).plusDays(1);
        while (date.getDayOfWeek() != DayOfWeek.MONDAY) {
            date = date.plusDays(1);
        }
        return date;
    }
}
