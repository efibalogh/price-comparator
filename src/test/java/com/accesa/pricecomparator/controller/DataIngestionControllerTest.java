package com.accesa.pricecomparator.controller;

import com.accesa.pricecomparator.dto.response.TriggeredAlertResponse;
import com.accesa.pricecomparator.service.AlertService;
import com.accesa.pricecomparator.service.CsvImporterService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.util.List;

import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(DataIngestionController.class)
@ActiveProfiles("test")
@SuppressWarnings("PMD.UnitTestContainsTooManyAsserts")
class DataIngestionControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private CsvImporterService csvImporterService;

    @MockitoBean
    private AlertService alertService;

    private TriggeredAlertResponse testTriggeredAlert;

    @BeforeEach
    void setUp() {
        testTriggeredAlert = TriggeredAlertResponse.builder()
                .productName("Test Product")
                .store("Test Store")
                .targetPrice(new BigDecimal("10.99"))
                .currentPrice(new BigDecimal("9.99"))
                .currency("RON")
                .message("Price dropped to 9.99 RON")
                .build();
    }

    @Test
    void whenNoTriggeredAlerts_expectOk_fromImportCsvData() throws Exception {
        // Given
        String directoryPath = "/test/directory";
        doNothing().when(csvImporterService).importDataFrom(directoryPath);
        when(alertService.checkAllAlertsForLatestPrices()).thenReturn(List.of());

        // When & Then
        mockMvc.perform(post("/api/data/import")
                        .param("directoryPath", directoryPath))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("CSV data import completed for directory: " + directoryPath))
                .andExpect(jsonPath("$.alertsTriggered").value(0))
                .andExpect(jsonPath("$.triggeredAlerts").doesNotExist());

        verify(csvImporterService).importDataFrom(directoryPath);
        verify(alertService).checkAllAlertsForLatestPrices();
    }

    @Test
    void whenTriggeredAlertsExist_expectOk_fromImportCsvData() throws Exception {
        // Given
        String directoryPath = "/test/directory";
        List<TriggeredAlertResponse> triggeredAlerts = List.of(testTriggeredAlert);
        
        doNothing().when(csvImporterService).importDataFrom(directoryPath);
        when(alertService.checkAllAlertsForLatestPrices()).thenReturn(triggeredAlerts);

        // When & Then
        mockMvc.perform(post("/api/data/import")
                        .param("directoryPath", directoryPath))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("CSV data import completed for directory: " + directoryPath))
                .andExpect(jsonPath("$.alertsTriggered").value(1))
                .andExpect(jsonPath("$.triggeredAlerts").isArray())
                .andExpect(jsonPath("$.triggeredAlerts[0].productName").value("Test Product"))
                .andExpect(jsonPath("$.triggeredAlerts[0].store").value("Test Store"))
                .andExpect(jsonPath("$.triggeredAlerts[0].targetPrice").value(10.99))
                .andExpect(jsonPath("$.triggeredAlerts[0].currentPrice").value(9.99))
                .andExpect(jsonPath("$.triggeredAlerts[0].currency").value("RON"))
                .andExpect(jsonPath("$.triggeredAlerts[0].message").value("Price dropped to 9.99 RON"));

        verify(csvImporterService).importDataFrom(directoryPath);
        verify(alertService).checkAllAlertsForLatestPrices();
    }

    @Test
    void whenMultipleTriggeredAlertsExist_expectOk_fromImportCsvData() throws Exception {
        // Given
        String directoryPath = "/test/directory";
        
        TriggeredAlertResponse alert1 = TriggeredAlertResponse.builder()
                .productName("Product 1")
                .store("Store 1")
                .targetPrice(new BigDecimal("5.99"))
                .currentPrice(new BigDecimal("4.99"))
                .currency("RON")
                .message("Price dropped to 4.99 RON")
                .build();

        TriggeredAlertResponse alert2 = TriggeredAlertResponse.builder()
                .productName("Product 2")
                .store("Store 2")
                .targetPrice(new BigDecimal("15.99"))
                .currentPrice(new BigDecimal("14.99"))
                .currency("RON")
                .message("Price dropped to 14.99 RON")
                .build();

        List<TriggeredAlertResponse> triggeredAlerts = List.of(alert1, alert2);
        
        doNothing().when(csvImporterService).importDataFrom(directoryPath);
        when(alertService.checkAllAlertsForLatestPrices()).thenReturn(triggeredAlerts);

        // When & Then
        mockMvc.perform(post("/api/data/import")
                        .param("directoryPath", directoryPath))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("CSV data import completed for directory: " + directoryPath))
                .andExpect(jsonPath("$.alertsTriggered").value(2))
                .andExpect(jsonPath("$.triggeredAlerts").isArray())
                .andExpect(jsonPath("$.triggeredAlerts.length()").value(2))
                .andExpect(jsonPath("$.triggeredAlerts[0].productName").value("Product 1"))
                .andExpect(jsonPath("$.triggeredAlerts[1].productName").value("Product 2"));

        verify(csvImporterService).importDataFrom(directoryPath);
        verify(alertService).checkAllAlertsForLatestPrices();
    }

    @Test
    void whenMissingDirectoryPath_expectBadRequest_fromImportCsvData() throws Exception {
        // When & Then
        mockMvc.perform(post("/api/data/import"))
                .andExpect(status().isBadRequest());

        verify(csvImporterService, never()).importDataFrom(anyString());
        verify(alertService, never()).checkAllAlertsForLatestPrices();
    }

    @Test
    void whenEmptyDirectoryPath_expectOk_fromImportCsvData() throws Exception {
        // Given
        String directoryPath = "";
        doNothing().when(csvImporterService).importDataFrom(directoryPath);
        when(alertService.checkAllAlertsForLatestPrices()).thenReturn(List.of());

        // When & Then
        mockMvc.perform(post("/api/data/import")
                        .param("directoryPath", directoryPath))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("CSV data import completed for directory: "))
                .andExpect(jsonPath("$.alertsTriggered").value(0));

        verify(csvImporterService).importDataFrom(directoryPath);
        verify(alertService).checkAllAlertsForLatestPrices();
    }
} 
