package com.accesa.pricecomparator.dto.request;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class BasketItemRequest {

    @NotBlank(message = "Product name cannot be blank")
    private String productName;

    @Min(value = 1, message = "Quantity must be at least 1")
    private int quantity;
}
