package com.accesa.pricecomparator.model;

import java.math.BigDecimal;

public record ProductSelection(ProductWithDiscount productWithDiscount, int quantity) {
    public BigDecimal getTotalCost() {
        return productWithDiscount.getEffectivePrice().multiply(BigDecimal.valueOf(quantity));
    }
}
