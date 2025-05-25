package com.accesa.pricecomparator.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ValuePerUnitResponse {

    private String productName;
    private String brand;
    private String store;
    private BigDecimal price;
    private BigDecimal packageQuantity;
    private String packageUnit;
    private BigDecimal valuePerUnit; // price / packageQuantity
    private String currency;
}
