package com.accesa.pricecomparator.dto.response;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class BestDiscountResponse {

    private String productName;
    private String brand;
    private String store;
    private LocalDate fromDate;
    private LocalDate toDate;
    private BigDecimal percentageOfDiscount;
}
