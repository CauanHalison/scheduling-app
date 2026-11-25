package com.github.cauanHalison.scheduling_api.eventtype.mapper;

import com.github.cauanHalison.scheduling_api.eventtype.domain.EventTypeEntity;
import com.github.cauanHalison.scheduling_api.eventtype.dto.EventTypeRequest;
import com.github.cauanHalison.scheduling_api.eventtype.dto.EventTypeResponse;
import com.github.cauanHalison.scheduling_api.eventtype.dto.PublicEventTypeResponse;
import org.springframework.stereotype.Component;

@Component
public class EventTypeMapper {
    public EventTypeEntity toEntity(EventTypeRequest eventType) {
        EventTypeEntity entity = new EventTypeEntity();
        entity.setTitle(eventType.title());
        entity.setEventLink(eventType.eventLink());
        entity.setDescription(eventType.description());
        entity.setDurationMinutes(eventType.durationMinutes());
        entity.setBufferAfterMinutes(eventType.bufferAfterMinutes());
        entity.setSlug(eventType.slug());

        return entity;
    }

    public EventTypeResponse toResponse(EventTypeEntity entity) {
        return new EventTypeResponse(
                entity.getId(),
                entity.getTitle(),
                entity.getSlug(),
                entity.getEventLink(),
                entity.getDescription(),
                entity.getDurationMinutes(),
                entity.getBufferAfterMinutes(),
                entity.getIsActive(),
                entity.getCreatedAt()
        );
    }

    public PublicEventTypeResponse toPublicResponse(EventTypeEntity entity) {
        return new PublicEventTypeResponse(
                entity.getId(),
                entity.getTitle(),
                entity.getSlug(),
                entity.getDescription(),
                entity.getDurationMinutes(),
                entity.getBufferAfterMinutes()
        );
    }

    public void updateEntity(EventTypeEntity entity, EventTypeRequest request) {
        entity.setTitle(request.title());
        entity.setSlug(request.slug());
        entity.setEventLink(request.eventLink());
        entity.setDescription(request.description());
        entity.setDurationMinutes(request.durationMinutes());
        entity.setBufferAfterMinutes(request.bufferAfterMinutes());
    }
}
