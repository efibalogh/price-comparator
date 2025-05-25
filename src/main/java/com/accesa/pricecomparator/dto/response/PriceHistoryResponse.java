package com.accesa.pricecomparator.dto.response;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class PriceHistoryResponse {

    private String productName;
    private String store;
    private List<PricePoint> priceHistory;

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class PricePoint {
        private LocalDate date;
        private BigDecimal price;
    }
}
