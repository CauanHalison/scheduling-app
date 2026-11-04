package com.github.cauanHalison.scheduling_api.schedule.service;

import com.github.cauanHalison.scheduling_api.exception.exceptions.ResourceNotFoundException;
import com.github.cauanHalison.scheduling_api.identity.domain.ProviderEntity;
import com.github.cauanHalison.scheduling_api.identity.repository.ProviderRepository;
import com.github.cauanHalison.scheduling_api.schedule.domain.ScheduleOverrideEntity;
import com.github.cauanHalison.scheduling_api.schedule.dto.ScheduleOverrideRequest;
import com.github.cauanHalison.scheduling_api.schedule.dto.ScheduleOverrideResponse;
import com.github.cauanHalison.scheduling_api.schedule.mapper.ScheduleOverrideMapper;
import com.github.cauanHalison.scheduling_api.schedule.repository.ScheduleOverrideRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ScheduleOverrideServiceTest {
    @Mock
    private ScheduleOverrideRepository scheduleOverrideRepository;

    @Mock
    private ProviderRepository providerRepository;

    @Spy
    private ScheduleOverrideMapper scheduleOverrideMapper = new ScheduleOverrideMapper();

    @InjectMocks
    private ScheduleOverrideService scheduleOverrideService;

    private ProviderEntity authenticatedProvider;

    @BeforeEach
    void setUp() {
        createSampleProvider();
    }

    @Test
    @DisplayName("Should return future schedule overrides from provider")
    void shouldReturnFutureOverridesFromProvider() {
        ScheduleOverrideEntity entity = createSampleScheduleOverrideEntity();

        when(scheduleOverrideRepository.findAllNextOverrides(eq(authenticatedProvider.getId()), any(Instant.class)))
                .thenReturn(List.of(entity));

        List<ScheduleOverrideResponse> response = scheduleOverrideService.findAllFuture(authenticatedProvider.getId());

        verify(scheduleOverrideRepository).findAllNextOverrides(eq(authenticatedProvider.getId()), any(Instant.class));

        assertThat(response).isNotNull();
        assertThat(response).hasSize(1);
        assertThat(response.getFirst().id()).isEqualTo(entity.getId());
        assertThat(response.getFirst().startTime()).isEqualTo(entity.getStartTime());
        assertThat(response.getFirst().endTime()).isEqualTo(entity.getEndTime());
        assertThat(response.getFirst().reason()).isEqualTo(entity.getReason());
    }

    @Test
    @DisplayName("Should create a new schedule override")
    void shouldCreateScheduleOverride() {
        ScheduleOverrideRequest request = createSampleScheduleOverrideRequest();
        UUID generatedId = UUID.randomUUID();

        when(providerRepository.getReferenceById(authenticatedProvider.getId()))
                .thenReturn(authenticatedProvider);
        when(scheduleOverrideRepository.saveAndFlush(any(ScheduleOverrideEntity.class)))
                .thenAnswer(invocation -> {
                    ScheduleOverrideEntity saved = invocation.getArgument(0);
                    saved.setId(generatedId);
                    return saved;
                });

        ScheduleOverrideResponse response = scheduleOverrideService.create(request, authenticatedProvider.getId());

        ArgumentCaptor<ScheduleOverrideEntity> captor = ArgumentCaptor.forClass(ScheduleOverrideEntity.class);
        verify(providerRepository).getReferenceById(authenticatedProvider.getId());
        verify(scheduleOverrideRepository).saveAndFlush(captor.capture());

        assertThat(captor.getValue().getProvider()).isSameAs(authenticatedProvider);
        assertThat(response).isNotNull();
        assertThat(response.id()).isEqualTo(generatedId);
        assertThat(response.startTime()).isEqualTo(request.startTime());
        assertThat(response.endTime()).isEqualTo(request.endTime());
        assertThat(response.reason()).isEqualTo(request.reason());
    }

    @Test
    @DisplayName("Should delete an existing schedule override")
    void shouldDeleteExistingScheduleOverride() {
        ScheduleOverrideEntity entity = createSampleScheduleOverrideEntity();

        when(scheduleOverrideRepository.findByIdAndProviderId(entity.getId(), authenticatedProvider.getId()))
                .thenReturn(Optional.of(entity));

        scheduleOverrideService.delete(entity.getId(), authenticatedProvider.getId());

        verify(scheduleOverrideRepository).findByIdAndProviderId(entity.getId(), authenticatedProvider.getId());
        verify(scheduleOverrideRepository).delete(entity);
    }

    @Test
    @DisplayName("Should throw ResourceNotFoundException when deleting a non-existent schedule override")
    void shouldThrowNotFoundWhenDeletingNonExistentScheduleOverride() {
        UUID nonExistentId = UUID.randomUUID();

        when(scheduleOverrideRepository.findByIdAndProviderId(nonExistentId, authenticatedProvider.getId()))
                .thenReturn(Optional.empty());

        assertThatThrownBy(() -> scheduleOverrideService.delete(nonExistentId, authenticatedProvider.getId()))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessage("Exceção não encontrada na base de dados");

        verify(scheduleOverrideRepository).findByIdAndProviderId(nonExistentId, authenticatedProvider.getId());
        verify(scheduleOverrideRepository, never()).delete(any());
    }

    private ScheduleOverrideEntity createSampleScheduleOverrideEntity() {
        ScheduleOverrideEntity scheduleOverrideEntity = new ScheduleOverrideEntity();
        scheduleOverrideEntity.setId(UUID.randomUUID());
        scheduleOverrideEntity.setProvider(authenticatedProvider);
        scheduleOverrideEntity.setStartTime(Instant.now().truncatedTo(ChronoUnit.SECONDS));
        scheduleOverrideEntity.setEndTime(Instant.now().truncatedTo(ChronoUnit.SECONDS).plus(1, ChronoUnit.DAYS));
        scheduleOverrideEntity.setReason("test");
        return scheduleOverrideEntity;
    }

    private ScheduleOverrideRequest createSampleScheduleOverrideRequest() {
        Instant base = Instant.now().truncatedTo(ChronoUnit.SECONDS);
        Instant start = base.plus(1, ChronoUnit.DAYS);
        Instant end = start.plus(1, ChronoUnit.DAYS);

        return new ScheduleOverrideRequest(
                start,
                end,
                "test"
        );
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
