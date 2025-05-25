package com.accesa.pricecomparator.controller;

import com.accesa.pricecomparator.dto.request.BasketItemRequest;
import com.accesa.pricecomparator.dto.response.OptimizedBasketResponse;
import com.accesa.pricecomparator.model.Product;
import com.accesa.pricecomparator.model.ShoppingList;
import com.accesa.pricecomparator.service.BasketService;
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
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(BasketController.class)
@ActiveProfiles("test")
class BasketControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private BasketService basketService;

    @Autowired
    private ObjectMapper objectMapper;

    private List<BasketItemRequest> testBasketItems;
    private OptimizedBasketResponse testOptimizedBasket;

    @BeforeEach
    void setUp() {
        testBasketItems = List.of(
                new BasketItemRequest("Product 1", 2),
                new BasketItemRequest("Product 2", 1)
        );

        Product product1 = Product.builder()
                .id(1L)
                .productId("P001")
                .name("Product 1")
                .category("Category 1")
                .brand("Brand 1")
                .store("Store 1")
                .price(new BigDecimal("9.99"))
                .currency("RON")
                .priceDate(LocalDate.now())
                .packageQuantity(BigDecimal.ONE)
                .packageUnit("buc")
                .build();

        Product product2 = Product.builder()
                .id(2L)
                .productId("P002")
                .name("Product 2")
                .category("Category 2")
                .brand("Brand 2")
                .store("Store 1")
                .price(new BigDecimal("15.99"))
                .currency("RON")
                .priceDate(LocalDate.now())
                .packageQuantity(BigDecimal.ONE)
                .packageUnit("buc")
                .build();

        ShoppingList shoppingList = ShoppingList.builder()
                .storeName("Store 1")
                .products(List.of(product1, product2))
                .itemCount(2)
                .originalCost(new BigDecimal("35.97"))
                .costAfterDiscounts(new BigDecimal("32.97"))
                .savings(new BigDecimal("3.00"))
                .build();

        testOptimizedBasket = OptimizedBasketResponse.builder()
                .totalOriginalCost(new BigDecimal("35.97"))
                .totalCostAfterDiscounts(new BigDecimal("32.97"))
                .totalSavings(new BigDecimal("3.00"))
                .storeShoppingLists(List.of(shoppingList))
                .build();
    }

    @Test
    void whenWithoutDate_expectOptimizedBasketReturned_fromOptimizeShoppingBasket() throws Exception {
        // Given
        when(basketService.optimize(eq(testBasketItems), eq(LocalDate.now())))
                .thenReturn(testOptimizedBasket);

        // When & Then
        mockMvc.perform(post("/api/basket/optimize")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(testBasketItems)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalOriginalCost").value(35.97))
                .andExpect(jsonPath("$.totalCostAfterDiscounts").value(32.97))
                .andExpect(jsonPath("$.totalSavings").value(3.00))
                .andExpect(jsonPath("$.storeShoppingLists").isArray())
                .andExpect(jsonPath("$.storeShoppingLists.length()").value(1))
                .andExpect(jsonPath("$.storeShoppingLists[0].storeName").value("Store 1"))
                .andExpect(jsonPath("$.storeShoppingLists[0].itemCount").value(2))
                .andExpect(jsonPath("$.storeShoppingLists[0].originalCost").value(35.97))
                .andExpect(jsonPath("$.storeShoppingLists[0].costAfterDiscounts").value(32.97))
                .andExpect(jsonPath("$.storeShoppingLists[0].savings").value(3.00));

        verify(basketService).optimize(eq(testBasketItems), eq(LocalDate.now()));
    }

    @Test
    void whenValidDate_expectOptimizedBasketReturned_fromOptimizeShoppingBasket() throws Exception {
        // Given
        LocalDate testDate = LocalDate.of(2024, 1, 15);
        when(basketService.optimize(eq(testBasketItems), eq(testDate)))
                .thenReturn(testOptimizedBasket);

        // When & Then
        mockMvc.perform(post("/api/basket/optimize")
                        .param("date", "2024-01-15")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(testBasketItems)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalOriginalCost").value(35.97))
                .andExpect(jsonPath("$.totalCostAfterDiscounts").value(32.97))
                .andExpect(jsonPath("$.totalSavings").value(3.00))
                .andExpect(jsonPath("$.storeShoppingLists").isArray())
                .andExpect(jsonPath("$.storeShoppingLists.length()").value(1));

        verify(basketService).optimize(eq(testBasketItems), eq(testDate));
    }

    @Test
    void whenEmptyBasket_expectEmptyOptimizedBasketReturned_fromOptimizeShoppingBasket() throws Exception {
        // Given
        List<BasketItemRequest> emptyBasket = List.of();
        OptimizedBasketResponse emptyResponse = OptimizedBasketResponse.builder()
                .totalOriginalCost(BigDecimal.ZERO)
                .totalCostAfterDiscounts(BigDecimal.ZERO)
                .totalSavings(BigDecimal.ZERO)
                .storeShoppingLists(List.of())
                .build();

        when(basketService.optimize(eq(emptyBasket), any(LocalDate.class)))
                .thenReturn(emptyResponse);

        // When & Then
        mockMvc.perform(post("/api/basket/optimize")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(emptyBasket)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalOriginalCost").value(0))
                .andExpect(jsonPath("$.totalCostAfterDiscounts").value(0))
                .andExpect(jsonPath("$.totalSavings").value(0))
                .andExpect(jsonPath("$.storeShoppingLists").isArray())
                .andExpect(jsonPath("$.storeShoppingLists.length()").value(0));

        verify(basketService).optimize(eq(emptyBasket), any(LocalDate.class));
    }

    @Test
    void whenSingleItem_expectOptimizedBasketReturned_fromOptimizeShoppingBasket() throws Exception {
        // Given
        List<BasketItemRequest> singleItemBasket = List.of(new BasketItemRequest("Single Product", 1));

        Product singleProduct = Product.builder()
                .id(1L)
                .name("Single Product")
                .store("Store 1")
                .price(new BigDecimal("10.00"))
                .currency("RON")
                .build();

        ShoppingList singleItemList = ShoppingList.builder()
                .storeName("Store 1")
                .products(List.of(singleProduct))
                .itemCount(1)
                .originalCost(new BigDecimal("10.00"))
                .costAfterDiscounts(new BigDecimal("10.00"))
                .savings(BigDecimal.ZERO)
                .build();

        OptimizedBasketResponse singleItemResponse = OptimizedBasketResponse.builder()
                .totalOriginalCost(new BigDecimal("10.00"))
                .totalCostAfterDiscounts(new BigDecimal("10.00"))
                .totalSavings(BigDecimal.ZERO)
                .storeShoppingLists(List.of(singleItemList))
                .build();

        when(basketService.optimize(eq(singleItemBasket), any(LocalDate.class)))
                .thenReturn(singleItemResponse);

        // When & Then
        mockMvc.perform(post("/api/basket/optimize")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(singleItemBasket)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalOriginalCost").value(10.00))
                .andExpect(jsonPath("$.totalCostAfterDiscounts").value(10.00))
                .andExpect(jsonPath("$.totalSavings").value(0))
                .andExpect(jsonPath("$.storeShoppingLists.length()").value(1));

        verify(basketService).optimize(eq(singleItemBasket), any(LocalDate.class));
    }

    @Test
    void whenBlankProductName_expectBadRequest_fromOptimizeShoppingBasket() throws Exception {
        // Given
        List<BasketItemRequest> invalidBasket = List.of(new BasketItemRequest("", 1));

        // When & Then
        mockMvc.perform(post("/api/basket/optimize")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(invalidBasket)))
                .andExpect(status().isBadRequest());

        verify(basketService, never()).optimize(any(), any(LocalDate.class));
    }

    @Test
    void whenZeroQuantity_expectBadRequest_fromOptimizeShoppingBasket() throws Exception {
        // Given
        List<BasketItemRequest> invalidBasket = List.of(new BasketItemRequest("Product", 0));

        // When & Then
        mockMvc.perform(post("/api/basket/optimize")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(invalidBasket)))
                .andExpect(status().isBadRequest());

        verify(basketService, never()).optimize(any(), any(LocalDate.class));
    }

    @Test
    void whenNegativeQuantity_expectBadRequest_fromOptimizeShoppingBasket() throws Exception {
        // Given
        List<BasketItemRequest> invalidBasket = List.of(new BasketItemRequest("Product", -1));

        // When & Then
        mockMvc.perform(post("/api/basket/optimize")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(invalidBasket)))
                .andExpect(status().isBadRequest());

        verify(basketService, never()).optimize(any(), any(LocalDate.class));
    }

    @Test
    void whenInvalidDateFormat_expectBadRequest_fromOptimizeShoppingBasket() throws Exception {
        // When & Then
        mockMvc.perform(post("/api/basket/optimize")
                        .param("date", "invalid-date")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(testBasketItems)))
                .andExpect(status().isBadRequest());

        verify(basketService, never()).optimize(any(), any(LocalDate.class));
    }

    @Test
    void whenInvalidJson_expectBadRequest_fromOptimizeShoppingBasket() throws Exception {
        // When & Then
        mockMvc.perform(post("/api/basket/optimize")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("\"invalid json\""))
                .andExpect(status().isBadRequest());

        verify(basketService, never()).optimize(any(), any(LocalDate.class));
    }

    @Test
    void whenMultipleStores_expectOptimizedBasketReturned_fromOptimizeShoppingBasket() throws Exception {
        // Given
        ShoppingList store1List = ShoppingList.builder()
                .storeName("Store 1")
                .products(List.of())
                .itemCount(1)
                .originalCost(new BigDecimal("10.00"))
                .costAfterDiscounts(new BigDecimal("9.00"))
                .savings(new BigDecimal("1.00"))
                .build();

        ShoppingList store2List = ShoppingList.builder()
                .storeName("Store 2")
                .products(List.of())
                .itemCount(1)
                .originalCost(new BigDecimal("15.00"))
                .costAfterDiscounts(new BigDecimal("14.00"))
                .savings(new BigDecimal("1.00"))
                .build();

        OptimizedBasketResponse multiStoreResponse = OptimizedBasketResponse.builder()
                .totalOriginalCost(new BigDecimal("25.00"))
                .totalCostAfterDiscounts(new BigDecimal("23.00"))
                .totalSavings(new BigDecimal("2.00"))
                .storeShoppingLists(List.of(store1List, store2List))
                .build();

        when(basketService.optimize(eq(testBasketItems), any(LocalDate.class)))
                .thenReturn(multiStoreResponse);

        // When & Then
        mockMvc.perform(post("/api/basket/optimize")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(testBasketItems)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalOriginalCost").value(25.00))
                .andExpect(jsonPath("$.totalCostAfterDiscounts").value(23.00))
                .andExpect(jsonPath("$.totalSavings").value(2.00))
                .andExpect(jsonPath("$.storeShoppingLists.length()").value(2))
                .andExpect(jsonPath("$.storeShoppingLists[0].storeName").value("Store 1"))
                .andExpect(jsonPath("$.storeShoppingLists[1].storeName").value("Store 2"));

        verify(basketService).optimize(eq(testBasketItems), any(LocalDate.class));
    }
} 
