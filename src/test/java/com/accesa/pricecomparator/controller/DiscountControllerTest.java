package com.accesa.pricecomparator.controller;

import com.accesa.pricecomparator.dto.response.BestDiscountResponse;
import com.accesa.pricecomparator.model.Discount;
import com.accesa.pricecomparator.service.DiscountService;
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

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(DiscountController.class)
@ActiveProfiles("test")
@SuppressWarnings("PMD.TooManyMethods")
class DiscountControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private DiscountService discountService;

    private Discount testDiscount;
    private BestDiscountResponse testBestDiscount;

    @BeforeEach
    void setUp() {
        testDiscount = Discount.builder()
                .id(1L)
                .productId("P001")
                .productName("Test Product")
                .productCategory("Test Category")
                .brand("Test Brand")
                .store("Test Store")
                .percentageOfDiscount(new BigDecimal("20.00"))
                .fromDate(LocalDate.now())
                .toDate(LocalDate.now().plusDays(7))
                .discountDate(LocalDate.now())
                .packageQuantity(BigDecimal.ONE)
                .packageUnit("buc")
                .build();

        testBestDiscount = new BestDiscountResponse(
                "Test Product",
                "Test Brand",
                "Test Store",
                LocalDate.now(),
                LocalDate.now().plusDays(7),
                new BigDecimal("20.00")
        );
    }

    @Test
    void whenDiscountsExist_expectListOfDiscountsReturned_fromGetAllDiscounts() throws Exception {
        // Given
        List<Discount> discounts = List.of(testDiscount);
        when(discountService.getAll()).thenReturn(discounts);

        // When & Then
        mockMvc.perform(get("/api/discounts"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].id").value(1))
                .andExpect(jsonPath("$[0].productId").value("P001"))
                .andExpect(jsonPath("$[0].productName").value("Test Product"))
                .andExpect(jsonPath("$[0].productCategory").value("Test Category"))
                .andExpect(jsonPath("$[0].brand").value("Test Brand"))
                .andExpect(jsonPath("$[0].store").value("Test Store"))
                .andExpect(jsonPath("$[0].percentageOfDiscount").value(20.00))
                .andExpect(jsonPath("$[0].packageQuantity").value(1))
                .andExpect(jsonPath("$[0].packageUnit").value("buc"));

        verify(discountService).getAll();
    }

    @Test
    void whenNoDiscountsExist_expectEmptyListReturned_fromGetAllDiscounts() throws Exception {
        // Given
        when(discountService.getAll()).thenReturn(List.of());

        // When & Then
        mockMvc.perform(get("/api/discounts"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$.length()").value(0));

        verify(discountService).getAll();
    }

    // --- Current Discounts Tests ---

    @Test
    void whenWithoutDate_expectListOfCurrentDiscountsReturned_fromGetCurrentDiscounts() throws Exception {
        // Given
        List<Discount> currentDiscounts = List.of(testDiscount);
        when(discountService.getCurrent(eq(LocalDate.now()))).thenReturn(currentDiscounts);

        // When & Then
        mockMvc.perform(get("/api/discounts/current"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].productName").value("Test Product"))
                .andExpect(jsonPath("$[0].percentageOfDiscount").value(20.00));

        verify(discountService).getCurrent(eq(LocalDate.now()));
    }

    @Test
    void whenValidDate_expectListOfCurrentDiscountsReturned_fromGetCurrentDiscounts() throws Exception {
        // Given
        LocalDate testDate = LocalDate.of(2024, 1, 15);
        List<Discount> currentDiscounts = List.of(testDiscount);
        when(discountService.getCurrent(testDate)).thenReturn(currentDiscounts);

        // When & Then
        mockMvc.perform(get("/api/discounts/current")
                        .param("date", "2024-01-15"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].productName").value("Test Product"));

        verify(discountService).getCurrent(testDate);
    }

    @Test
    void whenNoCurrentDiscountsExist_expectEmptyListReturned_fromGetCurrentDiscounts() throws Exception {
        // Given
        when(discountService.getCurrent(any(LocalDate.class))).thenReturn(List.of());

        // When & Then
        mockMvc.perform(get("/api/discounts/current"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$.length()").value(0));

        verify(discountService).getCurrent(any(LocalDate.class));
    }

    @Test
    void whenInvalidDateFormat_expectBadRequest_fromGetCurrentDiscounts() throws Exception {
        // When & Then
        mockMvc.perform(get("/api/discounts/current")
                        .param("date", "invalid-date"))
                .andExpect(status().isBadRequest());

        verify(discountService, never()).getCurrent(any(LocalDate.class));
    }

    // --- Best Discounts Tests ---

    @Test
    void whenMultipleDiscountsExist_expectListOfBestDiscountsReturned_fromGetBestDiscounts() throws Exception {
        // Given
        BestDiscountResponse discount1 = new BestDiscountResponse(
                "Product 1", "Brand 1", "Store 1",
                LocalDate.now(), LocalDate.now().plusDays(7),
                new BigDecimal("30.00")
        );
        BestDiscountResponse discount2 = new BestDiscountResponse(
                "Product 2", "Brand 2", "Store 2",
                LocalDate.now(), LocalDate.now().plusDays(5),
                new BigDecimal("25.00")
        );

        List<BestDiscountResponse> bestDiscounts = List.of(discount1, discount2);
        when(discountService.getBest(any(LocalDate.class), anyInt())).thenReturn(bestDiscounts);

        // When & Then
        mockMvc.perform(get("/api/discounts/best"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$.length()").value(2))
                .andExpect(jsonPath("$[0].productName").value("Product 1"))
                .andExpect(jsonPath("$[0].percentageOfDiscount").value(30.00))
                .andExpect(jsonPath("$[1].productName").value("Product 2"))
                .andExpect(jsonPath("$[1].percentageOfDiscount").value(25.00));

        verify(discountService).getBest(any(LocalDate.class), anyInt());
    }

    @Test
    void whenNoBestDiscountsExist_expectEmptyListReturned_fromGetBestDiscounts() throws Exception {
        // Given
        when(discountService.getBest(any(LocalDate.class), anyInt())).thenReturn(List.of());

        // When & Then
        mockMvc.perform(get("/api/discounts/best"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$.length()").value(0));

        verify(discountService).getBest(any(LocalDate.class), anyInt());
    }

    @Test
    void whenWithoutParameters_expectListOfBestDiscountsReturned_fromGetBestDiscounts() throws Exception {
        // Given
        List<BestDiscountResponse> bestDiscounts = List.of(testBestDiscount);
        when(discountService.getBest(eq(LocalDate.now()), eq(1000))).thenReturn(bestDiscounts);

        // When & Then
        mockMvc.perform(get("/api/discounts/best"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].productName").value("Test Product"))
                .andExpect(jsonPath("$[0].brand").value("Test Brand"))
                .andExpect(jsonPath("$[0].store").value("Test Store"))
                .andExpect(jsonPath("$[0].percentageOfDiscount").value(20.00));

        verify(discountService).getBest(eq(LocalDate.now()), eq(1000));
    }

    @Test
    void whenValidDateAndLimitAreSet_expectListOfBestDiscountsReturned_fromGetBestDiscounts() throws Exception {
        // Given
        LocalDate testDate = LocalDate.of(2024, 1, 15);
        int customLimit = 25;
        List<BestDiscountResponse> bestDiscounts = List.of(testBestDiscount);
        when(discountService.getBest(testDate, customLimit)).thenReturn(bestDiscounts);

        // When & Then
        mockMvc.perform(get("/api/discounts/best")
                        .param("date", "2024-01-15")
                        .param("limit", String.valueOf(customLimit)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$.length()").value(1));

        verify(discountService).getBest(testDate, customLimit);
    }

    @Test
    void whenValidDateIsSet_expectListOfBestDiscountsReturned_fromGetBestDiscounts() throws Exception {
        // Given
        LocalDate testDate = LocalDate.of(2024, 1, 15);
        List<BestDiscountResponse> bestDiscounts = List.of(testBestDiscount);
        when(discountService.getBest(testDate, 1000)).thenReturn(bestDiscounts);

        // When & Then
        mockMvc.perform(get("/api/discounts/best")
                        .param("date", "2024-01-15"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$.length()").value(1));

        verify(discountService).getBest(testDate, 1000);
    }

    @Test
    void whenInvalidDateFormat_expectBadRequest_fromGetBestDiscounts() throws Exception {
        // When & Then
        mockMvc.perform(get("/api/discounts/best")
                        .param("date", "invalid-date"))
                .andExpect(status().isBadRequest());

        verify(discountService, never()).getBest(any(LocalDate.class), anyInt());
    }

    @Test
    void whenLimitIsSet_expectListOfBestDiscountsReturned_fromGetBestDiscounts() throws Exception {
        // Given
        int customLimit = 50;
        List<BestDiscountResponse> bestDiscounts = List.of(testBestDiscount);
        when(discountService.getBest(eq(LocalDate.now()), eq(customLimit))).thenReturn(bestDiscounts);

        // When & Then
        mockMvc.perform(get("/api/discounts/best")
                        .param("limit", String.valueOf(customLimit)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$.length()").value(1));

        verify(discountService).getBest(eq(LocalDate.now()), eq(customLimit));
    }

    @Test
    void whenNegativeLimitIsSet_expectBadRequest_fromGetBestDiscounts() throws Exception {
        // When & Then
        mockMvc.perform(get("/api/discounts/best")
                        .param("limit", "-10"))
                .andExpect(status().isBadRequest());

        verify(discountService, never()).getBest(any(LocalDate.class), anyInt());
    }

    @Test
    void whenInvalidLimitFormat_expectBadRequest_fromGetBestDiscounts() throws Exception {
        // When & Then
        mockMvc.perform(get("/api/discounts/best")
                        .param("limit", "invalid-limit"))
                .andExpect(status().isBadRequest());

        verify(discountService, never()).getBest(any(LocalDate.class), anyInt());
    }

    // --- New Discounts Tests ---

    @Test
    void whenMultipleDiscountsExist_expectListOfNewDiscountsReturned_fromGetNewDiscounts() throws Exception {
        // Given
        Discount discount1 = Discount.builder()
                .id(1L)
                .productName("New Product 1")
                .percentageOfDiscount(new BigDecimal("15.00"))
                .build();

        Discount discount2 = Discount.builder()
                .id(2L)
                .productName("New Product 2")
                .percentageOfDiscount(new BigDecimal("10.00"))
                .build();

        List<Discount> newDiscounts = List.of(discount1, discount2);
        when(discountService.getNew(anyInt())).thenReturn(newDiscounts);

        // When & Then
        mockMvc.perform(get("/api/discounts/new"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$.length()").value(2))
                .andExpect(jsonPath("$[0].productName").value("New Product 1"))
                .andExpect(jsonPath("$[0].percentageOfDiscount").value(15.00))
                .andExpect(jsonPath("$[1].productName").value("New Product 2"))
                .andExpect(jsonPath("$[1].percentageOfDiscount").value(10.00));

        verify(discountService).getNew(anyInt());
    }

    @Test
    void whenNoNewDiscountsExist_expectEmptyListReturned_fromGetNewDiscounts() throws Exception {
        // Given
        when(discountService.getNew(anyInt())).thenReturn(List.of());

        // When & Then
        mockMvc.perform(get("/api/discounts/new"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$.length()").value(0));

        verify(discountService).getNew(anyInt());
    }

    @Test
    void whenDaysBackIsSet_expectListOfNewDiscountsReturned_fromGetNewDiscounts() throws Exception {
        // Given
        int daysBack = 7;
        List<Discount> newDiscounts = List.of(testDiscount);
        when(discountService.getNew(daysBack)).thenReturn(newDiscounts);

        // When & Then
        mockMvc.perform(get("/api/discounts/new")
                        .param("daysBack", String.valueOf(daysBack)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$.length()").value(1));

        verify(discountService).getNew(daysBack);
    }

    @Test
    void whenDaysBackIsNotSet_expectListOfNewDiscountsReturned_fromGetNewDiscounts() throws Exception {
        // Given
        List<Discount> newDiscounts = List.of(testDiscount);
        when(discountService.getNew(1)).thenReturn(newDiscounts);

        // When & Then
        mockMvc.perform(get("/api/discounts/new"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].productName").value("Test Product"))
                .andExpect(jsonPath("$[0].percentageOfDiscount").value(20.00));

        verify(discountService).getNew(1);
    }

    @Test
    void whenInvalidDaysBackFormat_expectBadRequest_fromGetNewDiscounts() throws Exception {
        // When & Then
        mockMvc.perform(get("/api/discounts/new")
                        .param("daysBack", "invalid-days"))
                .andExpect(status().isBadRequest());

        verify(discountService, never()).getNew(anyInt());
    }
} 
