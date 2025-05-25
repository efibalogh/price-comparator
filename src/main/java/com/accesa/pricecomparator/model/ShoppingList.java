package com.accesa.pricecomparator.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ShoppingList {
    
    private String storeName;
    private List<Product> products;
    private int itemCount;
    private BigDecimal originalCost;
    private BigDecimal costAfterDiscounts;
    private BigDecimal savings;
} 
