package com.accesa.pricecomparator.controller;

import com.accesa.pricecomparator.dto.request.AlertRequest;
import com.accesa.pricecomparator.exception.ResourceNotFoundException;
import com.accesa.pricecomparator.model.Alert;
import com.accesa.pricecomparator.service.AlertService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(AlertController.class)
@ActiveProfiles("test")
@SuppressWarnings("PMD.UnitTestContainsTooManyAsserts")
class AlertControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private AlertService alertService;

    @Autowired
    private ObjectMapper objectMapper;

    private Alert testAlert;
    private AlertRequest testAlertRequest;

    @BeforeEach
    void setUp() {
        testAlert = Alert.builder()
                .id(1L)
                .productName("Test Product")
                .store("Test Store")
                .targetPrice(new BigDecimal("10.99"))
                .active(true)
                .creationDate(LocalDate.now())
                .build();

        testAlertRequest = new AlertRequest("Test Product", "Test Store", new BigDecimal("10.99"));
    }

    @Test
    void whenAlertsExist_expectCreated_fromCreateAlerts() throws Exception {
        // Given
        List<AlertRequest> requests = List.of(testAlertRequest);

        when(alertService.create(anyString(), anyString(), any(BigDecimal.class)))
                .thenReturn(testAlert);

        // When & Then
        mockMvc.perform(post("/api/alerts")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(requests)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$[0].id").value(1))
                .andExpect(jsonPath("$[0].productName").value("Test Product"))
                .andExpect(jsonPath("$[0].store").value("Test Store"))
                .andExpect(jsonPath("$[0].targetPrice").value(10.99))
                .andExpect(jsonPath("$[0].active").value(true));

        verify(alertService).create("Test Product", "Test Store", new BigDecimal("10.99"));
    }

    @Test
    void whenMultipleAlertsExist_expectCreated_fromCreateAlerts() throws Exception {
        // Given
        AlertRequest request1 = new AlertRequest("Product 1", "Store 1", new BigDecimal("5.99"));
        AlertRequest request2 = new AlertRequest("Product 2", "Store 2", new BigDecimal("15.99"));
        List<AlertRequest> requests = List.of(request1, request2);

        Alert alert1 = Alert.builder()
                .id(1L)
                .productName("Product 1")
                .store("Store 1")
                .targetPrice(new BigDecimal("5.99"))
                .active(true)
                .creationDate(LocalDate.now())
                .build();

        Alert alert2 = Alert.builder()
                .id(2L)
                .productName("Product 2")
                .store("Store 2")
                .targetPrice(new BigDecimal("15.99"))
                .active(true)
                .creationDate(LocalDate.now())
                .build();

        when(alertService.create("Product 1", "Store 1", new BigDecimal("5.99")))
                .thenReturn(alert1);
        when(alertService.create("Product 2", "Store 2", new BigDecimal("15.99")))
                .thenReturn(alert2);

        // When & Then
        mockMvc.perform(post("/api/alerts")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(requests)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$.length()").value(2))
                .andExpect(jsonPath("$[0].productName").value("Product 1"))
                .andExpect(jsonPath("$[1].productName").value("Product 2"));

        verify(alertService).create("Product 1", "Store 1", new BigDecimal("5.99"));
        verify(alertService).create("Product 2", "Store 2", new BigDecimal("15.99"));
    }

    @Test
    void whenBlankProductName_expectBadRequest_fromCreateAlerts() throws Exception {
        // Given
        AlertRequest invalidRequest = new AlertRequest("", "Test Store", new BigDecimal("10.99"));
        List<AlertRequest> requests = List.of(invalidRequest);

        // When & Then
        mockMvc.perform(post("/api/alerts")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(requests)))
                .andExpect(status().isBadRequest());

        verify(alertService, never()).create(anyString(), anyString(), any(BigDecimal.class));
    }

    @Test
    void whenBlankStore_expectBadRequest_fromCreateAlerts() throws Exception {
        // Given
        AlertRequest invalidRequest = new AlertRequest("Test Product", "", new BigDecimal("10.99"));
        List<AlertRequest> requests = List.of(invalidRequest);

        // When & Then
        mockMvc.perform(post("/api/alerts")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(requests)))
                .andExpect(status().isBadRequest());

        verify(alertService, never()).create(anyString(), anyString(), any(BigDecimal.class));
    }

    @Test
    void whenNegativeTargetPrice_expectBadRequest_fromCreateAlerts() throws Exception {
        // Given
        AlertRequest invalidRequest = new AlertRequest("Test Product", "Test Store", new BigDecimal("-1.00"));
        List<AlertRequest> requests = List.of(invalidRequest);

        // When & Then
        mockMvc.perform(post("/api/alerts")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(requests)))
                .andExpect(status().isBadRequest());

        verify(alertService, never()).create(anyString(), anyString(), any(BigDecimal.class));
    }

    @Test
    void whenZeroTargetPrice_expectBadRequest_fromCreateAlerts() throws Exception {
        // Given
        AlertRequest invalidRequest = new AlertRequest("Test Product", "Test Store", BigDecimal.ZERO);
        List<AlertRequest> requests = List.of(invalidRequest);

        // When & Then
        mockMvc.perform(post("/api/alerts")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(requests)))
                .andExpect(status().isBadRequest());

        verify(alertService, never()).create(anyString(), anyString(), any(BigDecimal.class));
    }

    @Test
    void whenAlertId_expectOk_fromActivateAlert() throws Exception {
        // Given
        Long alertId = 1L;
        doNothing().when(alertService).activate(alertId);

        // When & Then
        mockMvc.perform(put("/api/alerts/{id}/activate", alertId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$['200']").value("Price alert activated"));

        verify(alertService).activate(alertId);
    }

    @Test
    void whenAlertIdNotFound_expectNotFound_fromActivateAlert() throws Exception {
        // Given
        Long alertId = 999L;
        doThrow(new ResourceNotFoundException("Price alert not found with ID: " + alertId))
                .when(alertService).activate(alertId);

        // When & Then
        mockMvc.perform(put("/api/alerts/{id}/activate", alertId))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$['404']").value("Price alert not found with ID: " + alertId));

        verify(alertService).activate(alertId);
    }

    @Test
    void whenAlertId_expectOk_fromDeactivateAlert() throws Exception {
        // Given
        Long alertId = 1L;
        doNothing().when(alertService).deactivate(alertId);

        // When & Then
        mockMvc.perform(put("/api/alerts/{id}/deactivate", alertId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$['200']").value("Price alert deactivated"));

        verify(alertService).deactivate(alertId);
    }

    @Test
    void whenAlertIdNotFound_expectNotFound_fromDeactivateAlert() throws Exception {
        // Given
        Long alertId = 999L;
        doThrow(new ResourceNotFoundException("Price alert not found with ID: " + alertId))
                .when(alertService).deactivate(alertId);

        // When & Then
        mockMvc.perform(put("/api/alerts/{id}/deactivate", alertId))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$['404']").value("Price alert not found with ID: " + alertId));

        verify(alertService).deactivate(alertId);
    }

    @Test
    void whenEmptyList_expectCreated_fromCreateAlerts() throws Exception {
        // Given
        List<AlertRequest> emptyRequests = List.of();

        // When & Then
        mockMvc.perform(post("/api/alerts")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(emptyRequests)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$.length()").value(0));

        verify(alertService, never()).create(anyString(), anyString(), any(BigDecimal.class));
    }

    @Test
    void whenInvalidJson_expectBadRequest_fromCreateAlerts() throws Exception {
        // When & Then
        mockMvc.perform(post("/api/alerts")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("\"invalid json\""))
                .andExpect(status().isBadRequest());

        verify(alertService, never()).create(anyString(), anyString(), any(BigDecimal.class));
    }
} 
