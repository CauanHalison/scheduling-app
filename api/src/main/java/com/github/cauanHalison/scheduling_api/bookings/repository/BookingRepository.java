package com.github.cauanHalison.scheduling_api.bookings.repository;

import com.github.cauanHalison.scheduling_api.bookings.domain.BookingEntity;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.lang.NonNull;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface BookingRepository extends JpaRepository<BookingEntity, UUID>, JpaSpecificationExecutor<BookingEntity> {

    @Query("""
            SELECT b FROM BookingEntity b
            WHERE b.provider.id = :providerId
            AND b.status = com.github.cauanHalison.scheduling_api.bookings.domain.enums.BookingStatus.CONFIRMED
            AND b.startTime < :endRange
            AND b.blockedUntil > :startRange
            ORDER BY b.startTime ASC
            """)
    List<BookingEntity> findConfirmedOverlapping(
            @Param("providerId") UUID providerId,
            @Param("startRange") Instant startRange,
            @Param("endRange") Instant endRange
    );

    @EntityGraph(attributePaths = {"eventType"})
    Optional<BookingEntity> findByIdAndProviderId(UUID id, UUID providerId);

    @Override
    @NonNull
    @EntityGraph(attributePaths = {"eventType"})
    Page<BookingEntity> findAll(Specification<BookingEntity> spec, @NonNull Pageable pageable);

    @EntityGraph(attributePaths = {"eventType", "provider"})
    Optional<BookingEntity> findByCancelToken(UUID cancelToken);

    @EntityGraph(attributePaths = {"eventType", "provider"})
    List<BookingEntity> findAllByClientEmailIgnoreCaseOrderByStartTimeDesc(String clientEmail);
}