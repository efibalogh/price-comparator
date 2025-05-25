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
public class TriggeredAlertResponse {

    private String productName;
    private String store;
    private BigDecimal targetPrice;
    private BigDecimal currentPrice;
    private String currency;
    private String message;
}
