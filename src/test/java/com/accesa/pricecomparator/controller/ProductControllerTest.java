package com.accesa.pricecomparator.controller;

import com.accesa.pricecomparator.dto.response.PriceHistoryResponse;
import com.accesa.pricecomparator.dto.response.ValuePerUnitResponse;
import com.accesa.pricecomparator.exception.ResourceNotFoundException;
import com.accesa.pricecomparator.model.Product;
import com.accesa.pricecomparator.service.ProductService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(ProductController.class)
@ActiveProfiles("test")
@SuppressWarnings("PMD.TooManyMethods")
class ProductControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private ProductService productService;

    private Product testProduct;
    private ValuePerUnitResponse testValuePerUnit;
    private PriceHistoryResponse testPriceHistory;

    @BeforeEach
    void setUp() {
        testProduct = Product.builder()
                .id(1L)
                .productId("P001")
                .name("Test Product")
                .category("Test Category")
                .brand("Test Brand")
                .store("Test Store")
                .price(new BigDecimal("10.99"))
                .currency("RON")
                .priceDate(LocalDate.now())
                .packageQuantity(BigDecimal.ONE)
                .packageUnit("buc")
                .build();

        testValuePerUnit = ValuePerUnitResponse.builder()
                .productName("Test Product")
                .brand("Test Brand")
                .store("Test Store")
                .price(new BigDecimal("10.99"))
                .packageQuantity(BigDecimal.ONE)
                .packageUnit("buc")
                .valuePerUnit(new BigDecimal("10.99"))
                .currency("RON")
                .build();

        List<PriceHistoryResponse.PricePoint> pricePoints = List.of(
                new PriceHistoryResponse.PricePoint(LocalDate.now().minusDays(1), new BigDecimal("11.99")),
                new PriceHistoryResponse.PricePoint(LocalDate.now(), new BigDecimal("10.99"))
        );

        testPriceHistory = new PriceHistoryResponse("Test Product", "Test Store", pricePoints);
    }

    @Test
    void whenProductsExist_expectListOfProductsReturned_fromGetAllProducts() throws Exception {
        // Given
        List<Product> products = List.of(testProduct);
        when(productService.getAll()).thenReturn(products);

        // When & Then
        mockMvc.perform(get("/api/products"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].id").value(1))
                .andExpect(jsonPath("$[0].productId").value("P001"))
                .andExpect(jsonPath("$[0].name").value("Test Product"))
                .andExpect(jsonPath("$[0].category").value("Test Category"))
                .andExpect(jsonPath("$[0].brand").value("Test Brand"))
                .andExpect(jsonPath("$[0].store").value("Test Store"))
                .andExpect(jsonPath("$[0].price").value(10.99))
                .andExpect(jsonPath("$[0].currency").value("RON"));

        verify(productService).getAll();
    }

    @Test
    void whenNoProductsExist_expectEmptyListReturned_fromGetAllProducts() throws Exception {
        // Given
        when(productService.getAll()).thenReturn(List.of());

        // When & Then
        mockMvc.perform(get("/api/products"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$.length()").value(0));

        verify(productService).getAll();
    }

    @Test
    void whenProductExists_expectProductReturned_fromGetProductById() throws Exception {
        // Given
        Long productId = 1L;
        when(productService.getById(productId)).thenReturn(Optional.of(testProduct));

        // When & Then
        mockMvc.perform(get("/api/products/{id}", productId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.productId").value("P001"))
                .andExpect(jsonPath("$.name").value("Test Product"))
                .andExpect(jsonPath("$.category").value("Test Category"))
                .andExpect(jsonPath("$.brand").value("Test Brand"))
                .andExpect(jsonPath("$.store").value("Test Store"))
                .andExpect(jsonPath("$.price").value(10.99))
                .andExpect(jsonPath("$.currency").value("RON"));

        verify(productService).getById(productId);
    }

    @Test
    void whenProductDoesNotExist_expectResourceNotFoundException_fromGetProductById() throws Exception {
        // Given
        Long productId = 999L;
        when(productService.getById(productId))
                .thenThrow(new ResourceNotFoundException("Product not found with id: " + productId));

        // When & Then
        mockMvc.perform(get("/api/products/{id}", productId))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.message").value("Product not found with id: " + productId));

        verify(productService).getById(productId);
    }

    // --- Product Value Per Unit Tests ---

    @Test
    void whenWithoutDate_expectValuePerUnitListForCurrentDateReturned_fromGetValuePerUnit() throws Exception {
        // Given
        List<ValuePerUnitResponse> valuePerUnitList = List.of(testValuePerUnit);
        when(productService.getValuePerUnit(eq(LocalDate.now()))).thenReturn(valuePerUnitList);

        // When & Then
        mockMvc.perform(get("/api/products/value"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].productName").value("Test Product"))
                .andExpect(jsonPath("$[0].brand").value("Test Brand"))
                .andExpect(jsonPath("$[0].store").value("Test Store"))
                .andExpect(jsonPath("$[0].price").value(10.99))
                .andExpect(jsonPath("$[0].packageQuantity").value(1))
                .andExpect(jsonPath("$[0].packageUnit").value("buc"))
                .andExpect(jsonPath("$[0].valuePerUnit").value(10.99))
                .andExpect(jsonPath("$[0].currency").value("RON"));

        verify(productService).getValuePerUnit(eq(LocalDate.now()));
    }

    @Test
    void whenValidDate_expectValuePerUnitListReturned_fromGetValuePerUnit() throws Exception {
        // Given
        LocalDate testDate = LocalDate.of(2024, 1, 15);
        List<ValuePerUnitResponse> valuePerUnitList = List.of(testValuePerUnit);
        when(productService.getValuePerUnit(testDate)).thenReturn(valuePerUnitList);

        // When & Then
        mockMvc.perform(get("/api/products/value")
                        .param("date", "2024-01-15"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].productName").value("Test Product"));

        verify(productService).getValuePerUnit(testDate);
    }

    @Test
    void whenInvalidDateFormat_expectBadRequest_fromGetValuePerUnit() throws Exception {
        // When & Then
        mockMvc.perform(get("/api/products/value")
                        .param("date", "invalid-date"))
                .andExpect(status().isBadRequest());

        verify(productService, never()).getValuePerUnit(any(LocalDate.class));
    }

    @Test
    void whenNoMatchingValues_expectEmptyListReturned_fromGetValuePerUnit() throws Exception {
        // Given
        when(productService.getValuePerUnit(any(LocalDate.class))).thenReturn(List.of());

        // When & Then
        mockMvc.perform(get("/api/products/value"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$.length()").value(0));

        verify(productService).getValuePerUnit(any(LocalDate.class));
    }

    // --- Product Price History Tests ---

    @Test
    void whenNameFilterIsSet_expectPriceHistoryListReturned_fromGetPriceHistory() throws Exception {
        // Given
        List<PriceHistoryResponse> historyList = List.of(testPriceHistory);
        when(productService.getPriceHistory(eq("name"), eq("Test Product"), isNull(), isNull(), isNull()))
                .thenReturn(historyList);

        // When & Then
        mockMvc.perform(get("/api/products/history")
                        .param("filter", "name")
                        .param("value", "Test Product"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].productName").value("Test Product"))
                .andExpect(jsonPath("$[0].store").value("Test Store"))
                .andExpect(jsonPath("$[0].priceHistory").isArray())
                .andExpect(jsonPath("$[0].priceHistory.length()").value(2))
                .andExpect(jsonPath("$[0].priceHistory[0].price").value(11.99))
                .andExpect(jsonPath("$[0].priceHistory[1].price").value(10.99));

        verify(productService).getPriceHistory(eq("name"), eq("Test Product"), isNull(), isNull(), isNull());
    }

    @Test
    void whenCategoryFilterIsSet_expectPriceHistoryListReturned_fromGetPriceHistory() throws Exception {
        // Given
        List<PriceHistoryResponse> historyList = List.of(testPriceHistory);
        when(productService.getPriceHistory(eq("category"), eq("Test Category"), isNull(), isNull(), isNull()))
                .thenReturn(historyList);

        // When & Then
        mockMvc.perform(get("/api/products/history")
                        .param("filter", "category")
                        .param("value", "Test Category"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$.length()").value(1));

        verify(productService).getPriceHistory(eq("category"), eq("Test Category"), isNull(), isNull(), isNull());
    }

    @Test
    void whenBrandFilterIsSet_expectPriceHistoryListReturned_fromGetPriceHistory() throws Exception {
        // Given
        List<PriceHistoryResponse> historyList = List.of(testPriceHistory);
        when(productService.getPriceHistory(eq("brand"), eq("Test Brand"), isNull(), isNull(), isNull()))
                .thenReturn(historyList);

        // When & Then
        mockMvc.perform(get("/api/products/history")
                        .param("filter", "brand")
                        .param("value", "Test Brand"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$.length()").value(1));

        verify(productService).getPriceHistory(eq("brand"), eq("Test Brand"), isNull(), isNull(), isNull());
    }

    @Test
    void whenAllParametersAreSet_expectPriceHistoryListReturned_fromGetPriceHistory() throws Exception {
        // Given
        LocalDate startDate = LocalDate.of(2024, 1, 1);
        LocalDate endDate = LocalDate.of(2024, 1, 31);
        List<PriceHistoryResponse> historyList = List.of(testPriceHistory);
        
        when(productService.getPriceHistory(
                eq("name"), eq("Test Product"), eq("Test Store"), eq(startDate), eq(endDate)
                ))
                .thenReturn(historyList);

        // When & Then
        mockMvc.perform(get("/api/products/history")
                        .param("filter", "name")
                        .param("value", "Test Product")
                        .param("store", "Test Store")
                        .param("startDate", "2024-01-01")
                        .param("endDate", "2024-01-31"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$.length()").value(1));

        verify(productService).getPriceHistory(
                eq("name"), eq("Test Product"), eq("Test Store"), eq(startDate), eq(endDate)
        );
    }

    @Test
    void whenRequiredParametersAreMissing_expectBadRequest_fromGetPriceHistory() throws Exception {
        // When & Then
        mockMvc.perform(get("/api/products/history"))
                .andExpect(status().isBadRequest());

        verify(productService, never()).getPriceHistory(anyString(), anyString(), anyString(), any(), any());
    }

    @Test
    void whenDateFormatIsInvalid_expectBadRequest_fromGetPriceHistory() throws Exception {
        // When & Then
        mockMvc.perform(get("/api/products/history")
                        .param("filter", "name")
                        .param("value", "Test Product")
                        .param("startDate", "invalid-date"))
                .andExpect(status().isBadRequest());

        verify(productService, never()).getPriceHistory(anyString(), anyString(), anyString(), any(), any());
    }

    @Test
    void whenServiceThrowsIllegalArgumentException_expectBadRequest_fromGetPriceHistory() throws Exception {
        // Given
        when(productService.getPriceHistory(eq("invalid"), eq("Test Product"), isNull(), isNull(), isNull()))
                .thenThrow(new IllegalArgumentException("Invalid filter type"));

        // When & Then
        mockMvc.perform(get("/api/products/history")
                        .param("filter", "invalid")
                        .param("value", "Test Product"))
                .andExpect(status().isBadRequest());

        verify(productService).getPriceHistory(eq("invalid"), eq("Test Product"), isNull(), isNull(), isNull());
    }

    @Test
    void whenNoMatchingHistory_expectEmptyListReturned_fromGetPriceHistory() throws Exception {
        // Given
        when(productService.getPriceHistory(eq("name"), eq("Nonexistent Product"), isNull(), isNull(), isNull()))
                .thenReturn(List.of());

        // When & Then
        mockMvc.perform(get("/api/products/history")
                        .param("filter", "name")
                        .param("value", "Nonexistent Product"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$.length()").value(0));

        verify(productService).getPriceHistory(eq("name"), eq("Nonexistent Product"), isNull(), isNull(), isNull());
    }
} 
