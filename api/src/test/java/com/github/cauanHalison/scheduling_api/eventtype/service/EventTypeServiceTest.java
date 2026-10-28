package com.github.cauanHalison.scheduling_api.eventtype.service;

import com.github.cauanHalison.scheduling_api.eventtype.domain.EventTypeEntity;
import com.github.cauanHalison.scheduling_api.eventtype.dto.EventTypeRequest;
import com.github.cauanHalison.scheduling_api.eventtype.dto.EventTypeResponse;
import com.github.cauanHalison.scheduling_api.eventtype.mapper.EventTypeMapper;
import com.github.cauanHalison.scheduling_api.eventtype.repository.EventTypeRepository;
import com.github.cauanHalison.scheduling_api.exception.exceptions.ResourceConflictException;
import com.github.cauanHalison.scheduling_api.exception.exceptions.ResourceNotFoundException;
import com.github.cauanHalison.scheduling_api.identity.domain.ProviderEntity;
import com.github.cauanHalison.scheduling_api.identity.repository.ProviderRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class EventTypeServiceTest {
    @Mock
    private EventTypeRepository eventTypeRepository;

    @Mock
    private ProviderRepository providerRepository;

    @Spy
    private EventTypeMapper eventTypeMapper = new EventTypeMapper();

    @InjectMocks
    private EventTypeService eventTypeService;

    private ProviderEntity authenticatedProvider;

    @BeforeEach
    void setUp() {
        createSampleProvider();
    }

    @Test
    @DisplayName("Should create a new event type for an authenticated provider")
    void shouldCreateNewEventType() {
        EventTypeRequest request = new EventTypeRequest(
                "Event type test",
                "event-type-test",
                "event type test",
                50,
                5
        );

        when(eventTypeRepository.existsByProviderIdAndSlug(authenticatedProvider.getId(), request.slug())).thenReturn(false);
        when(providerRepository.getReferenceById(authenticatedProvider.getId())).thenReturn(authenticatedProvider);
        when(eventTypeRepository.save(any(EventTypeEntity.class))).thenAnswer(i -> i.getArgument(0));

        EventTypeResponse response = eventTypeService.create(request, authenticatedProvider.getId());

        ArgumentCaptor<EventTypeEntity> captor = ArgumentCaptor.forClass(EventTypeEntity.class);
        verify(eventTypeRepository).save(captor.capture());
        assertThat(captor.getValue().getProvider().getId()).isEqualTo(authenticatedProvider.getId());

        assertThat(response).isNotNull();
        assertThat(response.title()).isEqualTo("Event type test");
        assertThat(response.description()).isEqualTo("event type test");
        assertThat(response.slug()).isEqualTo("event-type-test");
        assertThat(response.durationMinutes()).isEqualTo(50);
        assertThat(response.bufferAfterMinutes()).isEqualTo(5);
        assertThat(response.isActive()).isTrue();
    }

    @Test
    @DisplayName("Should throw ResourceConflictException when creating an event type with an existing slug")
    void shouldThrowConflictWhenSlugAlreadyExistsOnCreate() {
        EventTypeRequest request = new EventTypeRequest(
                "Event type test",
                "duplicated-slug",
                "description",
                30,
                5
        );

        when(eventTypeRepository.existsByProviderIdAndSlug(authenticatedProvider.getId(), request.slug()))
                .thenReturn(true);

        assertThatThrownBy(() -> eventTypeService.create(request, authenticatedProvider.getId()))
                .isInstanceOf(ResourceConflictException.class);

        verify(eventTypeRepository, never()).save(any());
    }

    @Test
    @DisplayName("Should update an event type successfully")
    void shouldUpdateEventTypeSuccessfully() {
        EventTypeEntity originalEventType = createSampleEventType();
        UUID expectedEventTypeId = originalEventType.getId();

        EventTypeRequest request = new EventTypeRequest(
                "updated event type",
                "updated-event-type",
                "updated event type",
                10,
                2
        );

        when(eventTypeRepository.findByIdAndProviderId(expectedEventTypeId, authenticatedProvider.getId())).thenReturn(Optional.of(originalEventType));
        when(eventTypeRepository.existsByProviderIdAndSlugAndIdNot(authenticatedProvider.getId(), request.slug(), expectedEventTypeId)).thenReturn(false);
        when(eventTypeRepository.save(any(EventTypeEntity.class))).thenAnswer(i -> i.getArgument(0));

        EventTypeResponse response = eventTypeService.update(expectedEventTypeId, request, authenticatedProvider.getId());

        verify(eventTypeRepository).save(originalEventType);

        assertThat(response).isNotNull();
        assertThat(response.id()).isEqualTo(expectedEventTypeId);
        assertThat(response.title()).isEqualTo("updated event type");
        assertThat(response.description()).isEqualTo("updated event type");
        assertThat(response.slug()).isEqualTo("updated-event-type");
        assertThat(response.durationMinutes()).isEqualTo(10);
        assertThat(response.bufferAfterMinutes()).isEqualTo(2);
    }

    @Test
    @DisplayName("Should throw ResourceConflictException when updating an event type with a slug already in use")
    void shouldThrowConflictWhenSlugAlreadyExistsOnUpdate() {
        EventTypeEntity existingEvent = createSampleEventType();
        EventTypeRequest request = new EventTypeRequest(
                "Updated title",
                "slug-used-in-other-event",
                "description",
                30,
                5
        );

        when(eventTypeRepository.findByIdAndProviderId(existingEvent.getId(), authenticatedProvider.getId()))
                .thenReturn(Optional.of(existingEvent));
        when(eventTypeRepository.existsByProviderIdAndSlugAndIdNot(authenticatedProvider.getId(), request.slug(), existingEvent.getId()))
                .thenReturn(true);

        assertThatThrownBy(() -> eventTypeService.update(existingEvent.getId(), request, authenticatedProvider.getId()))
                .isInstanceOf(ResourceConflictException.class);

        verify(eventTypeRepository, never()).save(any());
    }

    @Test
    @DisplayName("Should throw ResourceNotFoundException when updating a non-existent event type")
    void shouldThrowNotFoundWhenUpdatingNonExistentEventType() {
        UUID randomEventId = UUID.randomUUID();
        EventTypeRequest request = new EventTypeRequest("Title", "slug-test", "desc", 30, 5);

        when(eventTypeRepository.findByIdAndProviderId(randomEventId, authenticatedProvider.getId()))
                .thenReturn(Optional.empty());

        assertThatThrownBy(() -> eventTypeService.update(randomEventId, request, authenticatedProvider.getId()))
                .isInstanceOf(ResourceNotFoundException.class);

        verify(eventTypeRepository, never()).save(any());
    }

    @Test
    @DisplayName("Should toggle the isActive status of an event type from true to false")
    void shouldToggleIsActiveFromTrueToFalse() {
        EventTypeEntity entity = createSampleEventType();

        when(eventTypeRepository.findByIdAndProviderId(entity.getId(), authenticatedProvider.getId())).thenReturn(Optional.of(entity));
        when(eventTypeRepository.save(any(EventTypeEntity.class))).thenAnswer(i -> i.getArgument(0));

        EventTypeResponse response = eventTypeService.toggleIsActive(entity.getId(), authenticatedProvider.getId());

        verify(eventTypeRepository).save(entity);

        assertThat(response).isNotNull();
        assertThat(response.isActive()).isFalse();
    }

    @Test
    @DisplayName("Should throw ResourceNotFoundException when toggling status of a non-existent event type")
    void shouldThrowNotFoundWhenTogglingNonExistentEventType() {
        UUID randomEventId = UUID.randomUUID();

        when(eventTypeRepository.findByIdAndProviderId(randomEventId, authenticatedProvider.getId()))
                .thenReturn(Optional.empty());

        assertThatThrownBy(() -> eventTypeService.toggleIsActive(randomEventId, authenticatedProvider.getId()))
                .isInstanceOf(ResourceNotFoundException.class);

        verify(eventTypeRepository, never()).save(any());
    }

    @Test
    @DisplayName("Should delete an existing event type")
    void shouldDeleteExistingEventType() {
        EventTypeEntity entity = createSampleEventType();

        when(eventTypeRepository.findByIdAndProviderId(entity.getId(), authenticatedProvider.getId())).thenReturn(Optional.of(entity));

        eventTypeService.delete(entity.getId(), authenticatedProvider.getId());

        verify(eventTypeRepository).delete(entity);
    }

    @Test
    @DisplayName("Should throw ResourceNotFoundException when deleting a non-existent event type")
    void shouldThrowNotFoundWhenDeletingNonExistentEventType() {
        UUID randomEventId = UUID.randomUUID();

        when(eventTypeRepository.findByIdAndProviderId(randomEventId, authenticatedProvider.getId()))
                .thenReturn(Optional.empty());

        assertThatThrownBy(() -> eventTypeService.delete(randomEventId, authenticatedProvider.getId()))
                .isInstanceOf(ResourceNotFoundException.class);

        verify(eventTypeRepository, never()).delete(any());
    }

    @Test
    @DisplayName("Should list all event types from the authenticated provider")
    void shouldListAllEventTypesFromProvider() {
        EventTypeEntity entity = createSampleEventType();

        when(eventTypeRepository.findAllByProviderIdOrderByCreatedAtDesc(authenticatedProvider.getId())).thenReturn(List.of(entity));

        List<EventTypeResponse> response = eventTypeService.findAll(authenticatedProvider.getId());

        verify(eventTypeRepository).findAllByProviderIdOrderByCreatedAtDesc(authenticatedProvider.getId());

        assertThat(response).isNotNull();
        assertThat(response).hasSize(1);
        assertThat(response.getFirst().id()).isEqualTo(entity.getId());
        assertThat(response.getFirst().slug()).isEqualTo(entity.getSlug());
    }

    @Test
    @DisplayName("Should find an event type by id from the authenticated provider")
    void shouldFindEventTypeByIdSuccessfully() {
        EventTypeEntity entity = createSampleEventType();

        when(eventTypeRepository.findByIdAndProviderId(entity.getId(), authenticatedProvider.getId()))
                .thenReturn(Optional.of(entity));

        EventTypeResponse response = eventTypeService.findById(entity.getId(), authenticatedProvider.getId());

        verify(eventTypeRepository).findByIdAndProviderId(entity.getId(), authenticatedProvider.getId());

        assertThat(response).isNotNull();
        assertThat(response.id()).isEqualTo(entity.getId());
        assertThat(response.title()).isEqualTo(entity.getTitle());
        assertThat(response.slug()).isEqualTo(entity.getSlug());
        assertThat(response.description()).isEqualTo(entity.getDescription());
        assertThat(response.durationMinutes()).isEqualTo(entity.getDurationMinutes());
        assertThat(response.bufferAfterMinutes()).isEqualTo(entity.getBufferAfterMinutes());
        assertThat(response.isActive()).isEqualTo(entity.getIsActive());
    }

    @Test
    @DisplayName("Should throw ResourceNotFoundException when finding a non-existent event type by id")
    void shouldThrowNotFoundWhenFindingNonExistentEventTypeById() {
        UUID randomEventId = UUID.randomUUID();

        when(eventTypeRepository.findByIdAndProviderId(randomEventId, authenticatedProvider.getId()))
                .thenReturn(Optional.empty());

        assertThatThrownBy(() -> eventTypeService.findById(randomEventId, authenticatedProvider.getId()))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessage("Tipo de evento não encontrado.");

        verify(eventTypeRepository).findByIdAndProviderId(randomEventId, authenticatedProvider.getId());
    }

    private EventTypeEntity createSampleEventType() {
        EventTypeEntity entity = new EventTypeEntity();
        entity.setId(UUID.randomUUID());
        entity.setProvider(authenticatedProvider);
        entity.setTitle("Original title");
        entity.setSlug("original-slug");
        entity.setDescription("Original description");
        entity.setDurationMinutes(30);
        entity.setBufferAfterMinutes(10);
        entity.setIsActive(true);
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
