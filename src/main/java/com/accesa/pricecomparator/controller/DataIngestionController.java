package com.accesa.pricecomparator.controller;

import com.accesa.pricecomparator.service.AlertService;
import com.accesa.pricecomparator.service.CsvImporterService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.concurrent.ConcurrentHashMap;

@Slf4j
@RestController
@RequestMapping("/api/data")
@RequiredArgsConstructor
public class DataIngestionController {

    private final CsvImporterService csvImporterService;
    private final AlertService alertService;

    @PostMapping("/import")
    public ResponseEntity<?> importCsvData(@RequestParam String directoryPath) {
        log.info("POST /api/data/import?directoryPath={}", directoryPath);

        // Import CSV data
        csvImporterService.importDataFrom(directoryPath);

        // Check for triggered price alerts after import
        var triggeredAlerts = alertService.checkAllAlertsForLatestPrices();

        // Create response
        var response = new ConcurrentHashMap<>();
        response.put("message", "CSV data import completed for directory: " + directoryPath);
        response.put("alertsTriggered", triggeredAlerts.size());

        if (!triggeredAlerts.isEmpty()) {
            response.put("triggeredAlerts", triggeredAlerts);
            log.info("Found {} triggered price alerts after CSV import", triggeredAlerts.size());
        }

        return ResponseEntity
                .status(HttpStatus.OK)
                .body(response);
    }
}
