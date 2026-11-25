package com.github.cauanHalison.scheduling_api.bookings.mapper;

import com.github.cauanHalison.scheduling_api.bookings.domain.BookingEntity;
import com.github.cauanHalison.scheduling_api.bookings.domain.enums.BookingStatus;
import com.github.cauanHalison.scheduling_api.bookings.dto.BookingConfirmationResponse;
import com.github.cauanHalison.scheduling_api.bookings.dto.GuestBookingResponse;
import com.github.cauanHalison.scheduling_api.bookings.dto.ProviderBookingResponse;
import com.github.cauanHalison.scheduling_api.bookings.dto.PublicEventDetailsResponse;
import com.github.cauanHalison.scheduling_api.eventtype.domain.EventTypeEntity;
import org.springframework.stereotype.Component;

@Component
public class BookingMapper {

    public PublicEventDetailsResponse toPublicEventDetails(EventTypeEntity eventType) {
        return new PublicEventDetailsResponse(
                eventType.getProvider().getName(),
                eventType.getProvider().getSlug(),
                eventType.getProvider().getTimeZone(),
                eventType.getTitle(),
                eventType.getSlug(),
                eventType.getDescription(),
                eventType.getDurationMinutes()
        );
    }

    public BookingConfirmationResponse toConfirmationResponse(BookingEntity entity) {
        return new BookingConfirmationResponse(
                entity.getId(),
                entity.getEventType().getTitle(),
                resolveAccessibleEventLink(entity),
                entity.getProvider().getName(),
                entity.getClientName(),
                entity.getClientEmail(),
                entity.getStartTime(),
                entity.getEndTime(),
                entity.getStatus(),
                entity.getCancelToken(),
                entity.getCreatedAt()
        );
    }

    public ProviderBookingResponse toProviderResponse(BookingEntity entity) {
        return new ProviderBookingResponse(
                entity.getId(),
                entity.getEventType().getId(),
                entity.getEventType().getTitle(),
                resolveAccessibleEventLink(entity),
                entity.getEventType().getDurationMinutes(),
                entity.getClientName(),
                entity.getClientEmail(),
                entity.getStartTime(),
                entity.getEndTime(),
                entity.getStatus(),
                entity.getCancellationReason(),
                entity.getCreatedAt()
        );
    }

    public GuestBookingResponse toGuestResponse(BookingEntity entity) {
        return new GuestBookingResponse(
                entity.getId(),
                entity.getEventType().getTitle(),
                entity.getEventType().getDescription(),
                resolveAccessibleEventLink(entity),
                entity.getEventType().getDurationMinutes(),
                entity.getProvider().getName(),
                entity.getClientName(),
                entity.getClientEmail(),
                entity.getStartTime(),
                entity.getEndTime(),
                entity.getStatus(),
                entity.getCancellationReason()
        );
    }

    private String resolveAccessibleEventLink(BookingEntity entity) {
        if (entity.getStatus() == BookingStatus.CANCELED) {
            return null;
        }
        return entity.getEventType().getEventLink();
    }
}
