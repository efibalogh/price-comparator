package com.accesa.pricecomparator.controller;

import com.accesa.pricecomparator.dto.response.PriceHistoryResponse;
import com.accesa.pricecomparator.dto.response.ValuePerUnitResponse;
import com.accesa.pricecomparator.exception.ResourceNotFoundException;
import com.accesa.pricecomparator.model.Product;
import com.accesa.pricecomparator.service.ProductService;
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
@RequestMapping("/api/products")
@RequiredArgsConstructor
public class ProductController {

    private final ProductService productService;

    @GetMapping
    public ResponseEntity<List<Product>> getAllProducts() {
        log.info("GET /api/products");
        var products = productService.getAll();
        return ResponseEntity
                .status(HttpStatus.OK)
                .body(products);
    }

    @GetMapping("/{id}")
    public ResponseEntity<Product> getProductById(@PathVariable("id") Long id) {
        log.info("GET /api/products/{}", id);
        var product = productService.getById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Product not found with id: " + id));
        return ResponseEntity
                .status(HttpStatus.OK)
                .body(product);
    }

    @GetMapping("/history")
    public ResponseEntity<List<PriceHistoryResponse>> getProductPriceHistory(
            @RequestParam String filter,
            @RequestParam String value,
            @RequestParam(required = false) String store,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate
    ) {
        log.info(
            "GET /api/products/history?filter={}&value={}&store={}&startDate={}&endDate={}",
            filter,
            value,
            store,
            startDate,
            endDate
        );
        var history = productService.getPriceHistory(filter, value, store, startDate, endDate);
        return ResponseEntity
                .status(HttpStatus.OK)
                .body(history);
    }

    @GetMapping("/value")
    public ResponseEntity<List<ValuePerUnitResponse>> getProductsValuePerUnit(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date
    ) {
        // Default to the current date if no date is provided
        var effectiveDate = (date == null) ? LocalDate.now() : date;
        log.info("GET /api/products/value?date={}", date);
        var valuePerUnit = productService.getValuePerUnit(effectiveDate);
        return ResponseEntity
                .status(HttpStatus.OK)
                .body(valuePerUnit);
    }
}
