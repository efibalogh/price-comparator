package com.accesa.pricecomparator.service;

import com.accesa.pricecomparator.dto.request.BasketItemRequest;
import com.accesa.pricecomparator.dto.response.OptimizedBasketResponse;

import java.time.LocalDate;
import java.util.List;

public interface BasketService {

    /**
     * Optimizes a shopping basket by finding the lowest price for each item across all stores,
     * considering discounts and grouping results by store.
     *
     * @param basketItems List of BasketItemDto containing product names and quantities
     * @param date The date for which to compare prices
     * @return Optimized basket with products grouped by store
     */
    OptimizedBasketResponse optimize(List<BasketItemRequest> basketItems, LocalDate date);
}
