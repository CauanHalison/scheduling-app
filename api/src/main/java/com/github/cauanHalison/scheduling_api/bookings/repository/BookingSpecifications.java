package com.github.cauanHalison.scheduling_api.bookings.repository;

import com.github.cauanHalison.scheduling_api.bookings.domain.BookingEntity;
import com.github.cauanHalison.scheduling_api.bookings.domain.enums.BookingStatus;
import jakarta.persistence.criteria.Order;
import jakarta.persistence.criteria.Predicate;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.data.jpa.repository.query.QueryUtils;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public final class BookingSpecifications {

    private BookingSpecifications() {
    }

    public static Specification<BookingEntity> withFilters(
            UUID providerId,
            BookingStatus status,
            Instant startDate,
            Instant endDate,
            Sort sort
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

            if (query != null && query.getResultType() != long.class && query.getResultType() != Long.class) {
                List<Order> orders = new ArrayList<>();
                orders.add(cb.asc(
                        cb.<Integer>selectCase()
                                .when(cb.equal(root.get("status"), BookingStatus.CONFIRMED), 0)
                                .otherwise(1)
                ));
                if(sort != null && sort.isSorted()) {
                    orders.addAll(QueryUtils.toOrders(sort, root, cb));
                }
                query.orderBy(orders);
            }

            return cb.and(predicates.toArray(new Predicate[0]));
        };
    }
}
