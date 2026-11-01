package com.github.cauanHalison.scheduling_api.schedule.service;

import com.github.cauanHalison.scheduling_api.exception.exceptions.ResourceNotFoundException;
import com.github.cauanHalison.scheduling_api.identity.domain.ProviderEntity;
import com.github.cauanHalison.scheduling_api.identity.repository.ProviderRepository;
import com.github.cauanHalison.scheduling_api.schedule.domain.AvailabilityScheduleEntity;
import com.github.cauanHalison.scheduling_api.schedule.dto.*;
import com.github.cauanHalison.scheduling_api.schedule.mapper.AvailabilityScheduleMapper;
import com.github.cauanHalison.scheduling_api.schedule.repository.AvailabilityScheduleRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.*;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static java.time.DayOfWeek.MONDAY;
import static java.time.DayOfWeek.TUESDAY;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AvailabilityScheduleServiceTest {
    @Mock
    private AvailabilityScheduleRepository availabilityScheduleRepository;

    @Spy
    private AvailabilityScheduleMapper availabilityScheduleMapper = new AvailabilityScheduleMapper();

    @Mock
    private ProviderRepository providerRepository;

    @InjectMocks
    private AvailabilityScheduleService availabilityScheduleService;

    private ProviderEntity authenticatedProvider;

    @BeforeEach
    void setUp() {
        createSampleProvider();
    }

    @Test
    @DisplayName("Should return availability schedule from a provider")
    void shouldReturnAvailabilityScheduleFromProvider() {
        AvailabilityScheduleEntity availabilityScheduleEntity = createSampleAvailabilitySchedule();

        when(providerRepository.findById(authenticatedProvider.getId())).thenReturn(Optional.of(authenticatedProvider));
        when(availabilityScheduleRepository.findAllByProviderId(authenticatedProvider.getId())).thenReturn(List.of(availabilityScheduleEntity));

        WeeklyScheduleResponse response = availabilityScheduleService.find(authenticatedProvider.getId());

        verify(providerRepository).findById(authenticatedProvider.getId());
        verify(availabilityScheduleRepository).findAllByProviderId(authenticatedProvider.getId());

        assertThat(response).isNotNull();
        assertThat(response.days()).hasSize(7);
        assertThat(response.days().getFirst().intervals().getFirst().startTime()).isEqualTo(LocalTime.of(8, 0));
        assertThat(response.days().getFirst().intervals().getFirst().endTime()).isEqualTo(LocalTime.of(12, 0));
    }

    @Test
    @DisplayName("Should throw ResourceNotFoundException when provider does not exist on find")
    void shouldThrowResourceNotFoundExceptionWhenProviderDoesNotExistOnFind() {
        UUID nonExistentProviderId = UUID.randomUUID();

        when(providerRepository.findById(nonExistentProviderId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> availabilityScheduleService.find(nonExistentProviderId))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessage("Prestador não encontrado na base de dados");

        verify(providerRepository).findById(nonExistentProviderId);
        verify(availabilityScheduleRepository, never()).findAllByProviderId(any());
    }

    @Test
    @DisplayName("Should replace the weekly schedule of a provider")
    void shouldReplaceWeeklySchedule() {
        WeeklyScheduleRequest request = new WeeklyScheduleRequest(List.of(
                new DayScheduleRequest(MONDAY, List.of(
                        new TimeIntervalRequest(LocalTime.of(8, 0), LocalTime.of(12, 0)),
                        new TimeIntervalRequest(LocalTime.of(13, 0), LocalTime.of(17, 0))
                )),
                new DayScheduleRequest(TUESDAY, List.of())
        ));

        when(providerRepository.findByIdForUpdate(authenticatedProvider.getId())).thenReturn(Optional.of(authenticatedProvider));
        when(availabilityScheduleRepository.saveAllAndFlush(anyList())).thenAnswer(invocation -> invocation.getArgument(0));

        WeeklyScheduleResponse response = availabilityScheduleService.replace(request, authenticatedProvider.getId());

        InOrder inOrder = Mockito.inOrder(providerRepository, availabilityScheduleRepository);
        inOrder.verify(providerRepository).findByIdForUpdate(authenticatedProvider.getId());
        inOrder.verify(availabilityScheduleRepository).deleteAllByProviderId(authenticatedProvider.getId());
        inOrder.verify(availabilityScheduleRepository).saveAllAndFlush(anyList());

        assertThat(response).isNotNull();
        assertThat(response.timeZone()).isEqualTo("America/Sao_Paulo");
        assertThat(response.days()).hasSize(7);

        DayScheduleResponse monday = response.days().getFirst();
        assertThat(monday.dayOfWeek()).isEqualTo(MONDAY);
        assertThat(monday.intervals()).hasSize(2);
        assertThat(monday.intervals().get(0).startTime()).isEqualTo(LocalTime.of(8, 0));
        assertThat(monday.intervals().get(0).endTime()).isEqualTo(LocalTime.of(12, 0));
        assertThat(monday.intervals().get(1).startTime()).isEqualTo(LocalTime.of(13, 0));
        assertThat(monday.intervals().get(1).endTime()).isEqualTo(LocalTime.of(17, 0));

        DayScheduleResponse tuesday = response.days().get(1);
        assertThat(tuesday.dayOfWeek()).isEqualTo(TUESDAY);
        assertThat(tuesday.intervals()).isEmpty();
    }

    @Test
    @DisplayName("Should throw ResourceNotFoundException when provider does not exist on replace")
    void shouldThrowResourceNotFoundExceptionWhenProviderDoesNotExistOnReplace() {
        UUID nonExistentProviderId = UUID.randomUUID();
        WeeklyScheduleRequest request = new WeeklyScheduleRequest(List.of());

        when(providerRepository.findByIdForUpdate(nonExistentProviderId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> availabilityScheduleService.replace(request, nonExistentProviderId))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessage("Prestador não encontrado na base de dados");

        verify(providerRepository).findByIdForUpdate(nonExistentProviderId);
        verify(availabilityScheduleRepository, never()).deleteAllByProviderId(any());
        verify(availabilityScheduleRepository, never()).saveAllAndFlush(any());
    }

    private AvailabilityScheduleEntity createSampleAvailabilitySchedule() {
        AvailabilityScheduleEntity entity = new AvailabilityScheduleEntity();
        entity.setId(UUID.randomUUID());
        entity.setProvider(authenticatedProvider);
        entity.setStartTime(LocalTime.of(8, 0));
        entity.setEndTime(LocalTime.of(12, 0));
        entity.setDayOfWeek(MONDAY);

        return entity;
    }

    private void createSampleProvider() {
        authenticatedProvider = new ProviderEntity();
        authenticatedProvider.setId(UUID.randomUUID());
        authenticatedProvider.setEmail("email");
        authenticatedProvider.setSlug("slug");
        authenticatedProvider.setName("name");
        authenticatedProvider.setTimeZone("America/Sao_Paulo");
        authenticatedProvider.setPasswordHash("password");
    }
}
