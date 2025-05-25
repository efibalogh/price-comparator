package com.accesa.pricecomparator.controller;

import com.accesa.pricecomparator.dto.request.AlertRequest;
import com.accesa.pricecomparator.exception.ResourceNotFoundException;
import com.accesa.pricecomparator.model.Alert;
import com.accesa.pricecomparator.service.AlertService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@Slf4j
@RestController
@RequestMapping("/api/alerts")
@RequiredArgsConstructor
public class AlertController {

    private final AlertService alertService;

    @PostMapping
    public ResponseEntity<List<Alert>> createAlerts(@RequestBody List<@Valid AlertRequest> requests) {
        log.info("POST /api/alerts - Creating {} price alert(s)", requests.size());

        var alerts = requests.stream()
                .map(request -> {
                    log.info("Creating alert for product: {}, store: {}, targetPrice: {}",
                            request.getProductName(),
                            request.getStore(),
                            request.getTargetPrice()
                    );
                    return alertService.create(
                            request.getProductName(),
                            request.getStore(),
                            request.getTargetPrice()
                    );
                })
                .toList();

        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(alerts);
    }

    @PutMapping("/{id}/activate")
    public ResponseEntity<Map<Integer, String>> activateAlert(@PathVariable("id") Long id) {
        try {
            log.info("Activating price alert with ID: {}", id);
            alertService.activate(id);
            return ResponseEntity
                    .status(HttpStatus.OK)
                    .body(Map.of(200, "Price alert activated"));
        } catch (ResourceNotFoundException e) {
            return ResponseEntity
                    .status(HttpStatus.NOT_FOUND)
                    .body(Map.of(404, "Price alert not found with ID: " + id));
        }
    }

    @PutMapping("/{id}/deactivate")
    public ResponseEntity<Map<Integer, String>> deactivateAlert(@PathVariable("id") Long id) {
        try {
            log.info("Deactivating price alert with ID: {}", id);
            alertService.deactivate(id);
            return ResponseEntity
                    .status(HttpStatus.OK)
                    .body(Map.of(200, "Price alert deactivated"));
        } catch (ResourceNotFoundException e) {
            log.error("Error deactivating price alert with ID {}: {}", id, e.getMessage());
            return ResponseEntity
                    .status(HttpStatus.NOT_FOUND)
                    .body(Map.of(404, "Price alert not found with ID: " + id));
        }
    }
}
