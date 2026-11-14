package com.github.cauanHalison.scheduling_api.bookings.repository;

import com.github.cauanHalison.scheduling_api.bookings.domain.BookingEntity;
import com.github.cauanHalison.scheduling_api.bookings.domain.enums.BookingStatus;
import jakarta.persistence.criteria.Predicate;
import org.springframework.data.jpa.domain.Specification;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public final class BookingSpecifications {

    private BookingSpecifications() {}

    public static Specification<BookingEntity> withFilters(
            UUID providerId,
            BookingStatus status,
            Instant startDate,
            Instant endDate
    ) {
        return (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();

            predicates.add(cb.equal(root.get("provider").get("id"), providerId));

            if (status != null) {
                predicates.add(cb.equal(root.get("status"), status));
            }
            if (startDate != null) {
                predicates.add(cb.greaterThanOrEqualTo(root.get("startTime"), startDate));
            }
            if (endDate != null) {
                predicates.add(cb.lessThanOrEqualTo(root.get("startTime"), endDate));
            }

            return cb.and(predicates.toArray(new Predicate[0]));
        };
    }
}
