package com.accesa.pricecomparator.model;

import lombok.Getter;

import java.math.BigDecimal;
import java.math.RoundingMode;

@Getter
public class ProductWithDiscount {
    private final Product product;
    private final Discount discount;
    private final BigDecimal effectivePrice;

    public ProductWithDiscount(Product product, Discount discount) {
        this.product = product;
        this.discount = discount;

        if (discount != null) {
            BigDecimal discountAmount = product.getPrice()
                    .multiply(discount.getPercentageOfDiscount())
                    .divide(BigDecimal.valueOf(100), 2, RoundingMode.HALF_UP);
            this.effectivePrice = product.getPrice().subtract(discountAmount);
        } else {
            this.effectivePrice = product.getPrice();
        }
    }
}
