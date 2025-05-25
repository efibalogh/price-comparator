package com.accesa.pricecomparator.model;

import lombok.Getter;

import java.math.BigDecimal;
import java.math.RoundingMode;

/**
 * Wrapper class that combines a Product with its applicable Discount to calculate effective pricing.
 * This class encapsulates the discount calculation logic and ensures consistent
 * price computation across the application.
 */
@Getter
public class ProductWithDiscount {
    private final Product product;
    private final Discount discount;
    private final BigDecimal effectivePrice;

    /**
     * Creates a ProductWithDiscount instance and calculates the effective price.
     * Discount Calculation Logic:
     * - If a discount is provided, calculates: originalPrice - (originalPrice * discountPercentage / 100)
     * - Uses HALF_UP rounding to 2 decimal places for currency precision
     * - If no discount is provided, effective price equals original price
     *
     * @param product The base product with original pricing
     * @param discount The applicable discount (can be null)
     */
    public ProductWithDiscount(Product product, Discount discount) {
        this.product = product;
        this.discount = discount;

        if (discount != null) {
            // Calculate discount amount: originalPrice * (discountPercentage / 100)
            BigDecimal discountAmount = product.getPrice()
                    .multiply(discount.getPercentageOfDiscount())
                    .divide(BigDecimal.valueOf(100), 2, RoundingMode.HALF_UP);
            
            // Effective price = original price - discount amount
            this.effectivePrice = product.getPrice().subtract(discountAmount);
        } else {
            // No discount applicable, use the original price
            this.effectivePrice = product.getPrice();
        }
    }
}
