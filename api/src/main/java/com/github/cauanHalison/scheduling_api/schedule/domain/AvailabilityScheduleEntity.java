package com.github.cauanHalison.scheduling_api.schedule.domain;

import com.github.cauanHalison.scheduling_api.identity.domain.ProviderEntity;
import jakarta.persistence.*;
import lombok.Data;
import lombok.ToString;

import java.time.DayOfWeek;
import java.time.Instant;
import java.time.LocalTime;
import java.util.UUID;

@Entity
@Data
@Table(name = "availability_schedule")
public class AvailabilityScheduleEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @JoinColumn(name = "provider_id",  nullable = false)
    @ManyToOne(fetch = FetchType.LAZY)
    @ToString.Exclude
    private ProviderEntity provider;

    @Column(name = "day_of_week", nullable = false)
    @Enumerated(EnumType.STRING)
    private DayOfWeek dayOfWeek;

    @Column(name = "start_time", nullable = false)
    private LocalTime startTime;

    @Column(name = "end_time", nullable = false)
    private LocalTime endTime;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @PrePersist
    public void prePersist() {
        this.createdAt = Instant.now();
    }
}
