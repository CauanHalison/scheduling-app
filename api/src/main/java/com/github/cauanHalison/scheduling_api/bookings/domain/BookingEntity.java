package com.github.cauanHalison.scheduling_api.bookings.domain;

import com.github.cauanHalison.scheduling_api.bookings.domain.enums.BookingStatus;
import com.github.cauanHalison.scheduling_api.eventtype.domain.EventTypeEntity;
import com.github.cauanHalison.scheduling_api.identity.domain.ProviderEntity;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "booking")
@Getter
@Setter
public class BookingEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "event_type_id",  nullable = false)
    private EventTypeEntity eventType;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "provider_id", nullable = false)
    private ProviderEntity provider;

    @Column(name = "client_name", nullable = false)
    private String clientName;

    @Column(name = "client_email", nullable = false)
    private String clientEmail;

    @Column(name = "start_time", nullable = false)
    private Instant startTime;

    @Column(name = "end_time", nullable = false)
    private Instant endTime;

    @Column(name = "blocked_until", nullable = false)
    private Instant blockedUntil;

    @Column(name = "status", nullable = false)
    @Enumerated(EnumType.STRING)
    private BookingStatus status;

    @Column(name = "cancel_token", nullable = false, unique = true, updatable = false)
    private UUID cancelToken;

    @Column(name = "cancellation_reason", length = 500)
    private String cancellationReason;

    @Column(name = "reminder_sent_at")
    private Instant reminderSentAt;

    @Version
    @Column(name = "version", nullable = false)
    private Long version;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @PrePersist
    public void prePersist() {
        if (this.cancelToken == null) {
            this.cancelToken = UUID.randomUUID();
        }
        if (this.status == null) {
            this.status = BookingStatus.CONFIRMED;
        }
        this.createdAt = Instant.now();
    }
}
