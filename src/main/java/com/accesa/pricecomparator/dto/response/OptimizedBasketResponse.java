package com.accesa.pricecomparator.dto.response;

import com.accesa.pricecomparator.model.ShoppingList;
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
public class OptimizedBasketResponse {
    
    private BigDecimal totalOriginalCost;
    private BigDecimal totalCostAfterDiscounts;
    private BigDecimal totalSavings;
    private List<ShoppingList> storeShoppingLists;
} 
