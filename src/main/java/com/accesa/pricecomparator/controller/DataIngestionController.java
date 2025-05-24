package com.accesa.pricecomparator.controller;

import com.accesa.pricecomparator.service.CsvImporterService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@Slf4j
@RestController
@RequestMapping("/api/data")
@RequiredArgsConstructor
public class DataIngestionController {

    private final CsvImporterService csvImporterService;

    @PostMapping("/import")
    public ResponseEntity<String> importCsvData(@RequestParam String directoryPath) {
        log.info("POST /api/data/import?directoryPath={}", directoryPath);
        csvImporterService.importDataFrom(directoryPath);
        return ResponseEntity
                .status(HttpStatus.OK)
                .body("CSV data import initiated for directory: " + directoryPath);
    }
}
