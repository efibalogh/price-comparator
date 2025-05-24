package com.accesa.pricecomparator.controller;

import com.accesa.pricecomparator.dto.response.BestDiscountResponse;
import com.accesa.pricecomparator.model.Discount;
import com.accesa.pricecomparator.service.DiscountService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;
import java.util.List;

@Slf4j
@RestController
@RequestMapping("/api/discounts")
@RequiredArgsConstructor
public class DiscountController {

    private final DiscountService discountService;

    @GetMapping
    public ResponseEntity<List<Discount>> getAllDiscounts() {
        log.info("GET /api/discounts");
        var discounts = discountService.getAll();
        return ResponseEntity
                .status(HttpStatus.OK)
                .body(discounts);
    }

    @GetMapping("/current")
    public ResponseEntity<List<Discount>> getCurrentDiscounts(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date
    ) {
        // Default to the current date if no date is provided
        var effectiveDate = (date == null) ? LocalDate.now() : date;
        log.info("GET /api/currentDiscounts/current?date={}", effectiveDate);
        var currentDiscounts = discountService.getCurrent(effectiveDate);
        return ResponseEntity
                .status(HttpStatus.OK)
                .body(currentDiscounts);
    }

    @GetMapping("/best")
    public ResponseEntity<List<BestDiscountResponse>> getBestDiscounts(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date,
            @RequestParam(defaultValue = "1000") int limit
    ) {
        // Default to the current date if no date is provided
        var effectiveDate = (date == null) ? LocalDate.now() : date;
        log.info("GET /api/discounts/best?date={}&limit={}", effectiveDate, limit);
        var bestDiscounts = discountService.getBest(effectiveDate, limit);
        return ResponseEntity
                .status(HttpStatus.OK)
                .body(bestDiscounts);
    }

    @GetMapping("/new")
    public ResponseEntity<List<Discount>> getNewDiscounts(@RequestParam(defaultValue = "1") int daysBack) {
        log.info("GET /api/discounts/new?daysBack={}", daysBack);
        var newDiscounts = discountService.getNew(daysBack);
        return ResponseEntity
                .status(HttpStatus.OK)
                .body(newDiscounts);
    }
}
