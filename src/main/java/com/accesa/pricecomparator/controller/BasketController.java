package com.accesa.pricecomparator.controller;

import com.accesa.pricecomparator.dto.request.BasketItemRequest;
import com.accesa.pricecomparator.dto.response.OptimizedBasketResponse;
import com.accesa.pricecomparator.service.BasketService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;

@Slf4j
@RestController
@RequestMapping("/api/basket")
@RequiredArgsConstructor
public class BasketController {

    private final BasketService basketService;

    @PostMapping("/optimize")
    public ResponseEntity<OptimizedBasketResponse> optimizeShoppingBasket(
            @RequestBody @Valid List<BasketItemRequest> basketItems,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date
    ) {
        // Default to the current date if no date is provided
        var effectiveDate = (date == null) ? LocalDate.now() : date;
        log.info("POST /api/basket/optimize?date={}", date);
        var optimizedBasket = basketService.optimize(basketItems, effectiveDate);
        return ResponseEntity
                .status(HttpStatus.OK)
                .body(optimizedBasket);
    }
}
