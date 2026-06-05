package com.ubs.billing.util;

import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;

import java.util.List;
import java.util.Set;

public final class PageableSortUtils {

    private PageableSortUtils() {
    }

    public static Pageable withAllowedSort(Pageable pageable, Set<String> allowedProperties, String defaultProperty) {
        Sort sort = pageable.getSort();
        if (sort.isUnsorted()) {
            return pageRequest(pageable, defaultSort(defaultProperty));
        }

        List<Sort.Order> validOrders = sort.stream()
                .filter(order -> allowedProperties.contains(order.getProperty()))
                .toList();

        if (validOrders.isEmpty()) {
            return pageRequest(pageable, defaultSort(defaultProperty));
        }

        return pageRequest(pageable, Sort.by(validOrders));
    }

    private static PageRequest pageRequest(Pageable pageable, Sort sort) {
        return PageRequest.of(pageable.getPageNumber(), pageable.getPageSize(), sort);
    }

    private static Sort defaultSort(String defaultProperty) {
        return Sort.by(Sort.Direction.DESC, defaultProperty);
    }
}
