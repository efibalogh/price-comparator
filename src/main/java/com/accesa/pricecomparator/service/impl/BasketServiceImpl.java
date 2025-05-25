package com.accesa.pricecomparator.service.impl;

import com.accesa.pricecomparator.dto.request.BasketItemRequest;
import com.accesa.pricecomparator.dto.response.OptimizedBasketResponse;
import com.accesa.pricecomparator.model.*;
import com.accesa.pricecomparator.repository.DiscountRepository;
import com.accesa.pricecomparator.repository.ProductRepository;
import com.accesa.pricecomparator.service.BasketService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class BasketServiceImpl implements BasketService {

    private final ProductRepository productRepository;
    private final DiscountRepository discountRepository;

    /**
     * Optimizes a shopping basket by finding the least expensive option for each item across all stores.
     * Algorithm Overview:
     * 1. Retrieve all products with their effective prices (original price minus applicable discounts)
     * 2. For each basket item, find the store offering the lowest effective price
     * 3. Group selected products by store to create optimized shopping lists
     * 4. Calculate total costs and savings
     * Note: This is a greedy algorithm that optimizes for individual item cost rather than 
     * considering factors like travel costs between stores or bulk discounts.
     */
    @Override
    public OptimizedBasketResponse optimize(List<BasketItemRequest> basketItems, LocalDate date) {
        log.info("Optimizing basket with {} items for date {}", basketItems.size(), date);
        
        // Step 1: Get all available products with their effective prices (including discounts)
        Map<String, List<ProductWithDiscount>> productsByName = getProductsWithDiscounts(basketItems, date);
        
        // Step 2: Find the least expensive option for each item
        // Using ConcurrentHashMap for thread safety in case of future parallel processing
        Map<String, List<ProductSelection>> storeSelections = new ConcurrentHashMap<>();
        BigDecimal totalOriginalCost = BigDecimal.ZERO;
        BigDecimal totalCostAfterDiscounts = BigDecimal.ZERO;
        
        for (BasketItemRequest item : basketItems) {
            List<ProductWithDiscount> availableProducts = productsByName.get(item.getProductName());
            if (availableProducts == null || availableProducts.isEmpty()) {
                log.warn("No products available for item: {}", item.getProductName());
                continue;
            }
            
            // Greedy selection: choose the least expensive option regardless of store
            // This may result in shopping at multiple stores but guarantees the lowest total cost
            ProductWithDiscount cheapest = availableProducts.stream()
                    .min(Comparator.comparing(ProductWithDiscount::getEffectivePrice))
                    .orElse(null);

            // Calculate costs for this item (original vs. effective price)
            BigDecimal originalItemCost = cheapest
                    .getProduct()
                    .getPrice()
                    .multiply(BigDecimal.valueOf(item.getQuantity()));
            BigDecimal effectiveItemCost = cheapest
                    .getEffectivePrice()
                    .multiply(BigDecimal.valueOf(item.getQuantity()));

            totalOriginalCost = totalOriginalCost.add(originalItemCost);
            totalCostAfterDiscounts = totalCostAfterDiscounts.add(effectiveItemCost);

            // Group products by store for shopping list generation
            String store = cheapest.getProduct().getStore();
            storeSelections.computeIfAbsent(store, k -> new ArrayList<>())
                    .add(new ProductSelection(cheapest, item.getQuantity()));

            log.debug("Selected {} from {} at effective price {} (quantity: {})",
                    item.getProductName(), store, cheapest.getEffectivePrice(), item.getQuantity());
        }
        
        // Step 3: Create store shopping lists
        List<ShoppingList> storeShoppingLists = createStoreShoppingLists(storeSelections);
        
        BigDecimal totalSavings = totalOriginalCost.subtract(totalCostAfterDiscounts);
        
        log.info("Optimization complete. Total original cost: {}, After discounts: {}, Savings: {}", 
                totalOriginalCost, totalCostAfterDiscounts, totalSavings);
        
        return OptimizedBasketResponse.builder()
                .totalOriginalCost(totalOriginalCost)
                .totalCostAfterDiscounts(totalCostAfterDiscounts)
                .totalSavings(totalSavings)
                .storeShoppingLists(storeShoppingLists)
                .build();
    }

    /**
     * Builds a comprehensive map of products with their effective prices after applying discounts.
     * The discount resolution strategy:
     * - Groups discounts by product name and store
     * - If multiple discounts exist for the same product/store combination, 
     *   selects the one with the highest percentage (most beneficial to customer)
     * - Only considers discounts that are active on the specified date
     */
    private Map<String, List<ProductWithDiscount>> getProductsWithDiscounts(
            List<BasketItemRequest> basketItems,
            LocalDate date
    ) {
        Map<String, List<ProductWithDiscount>> productsByName = new ConcurrentHashMap<>();
        
        // Build the discount lookup map for efficient access
        // Structure: productName -> storeName -> bestDiscount
        List<Discount> currentDiscounts = discountRepository.findActiveOnDate(date);
        Map<String, Map<String, Discount>> discountMap = currentDiscounts.stream()
                .collect(Collectors.groupingBy(
                    Discount::getProductName,
                    Collectors.toMap(
                        Discount::getStore,
                        discount -> discount,
                        // Conflict resolution: choose discount with the higher percentage
                        (d1, d2) -> d1.getPercentageOfDiscount().compareTo(d2.getPercentageOfDiscount()) > 0 ? d1 : d2
                    )
                ));

        // For each requested item, find all available products and apply discounts
        for (BasketItemRequest item : basketItems) {
            List<Product> availableProducts = productRepository.findByNameAndPriceDate(item.getProductName(), date);
            
            if (!availableProducts.isEmpty()) {
                List<ProductWithDiscount> productsWithDiscounts = availableProducts.stream()
                        .map(product -> {
                            // Look up the applicable discount for this product/store combination
                            Discount applicableDiscount = null;
                            if (discountMap.containsKey(product.getName())
                                && discountMap.get(product.getName()).containsKey(product.getStore())) {
                                applicableDiscount = discountMap.get(product.getName()).get(product.getStore());
                            }
                            return new ProductWithDiscount(product, applicableDiscount);
                        })
                        .collect(Collectors.toList());
                
                productsByName.put(item.getProductName(), productsWithDiscounts);
                continue;
            }
            log.warn("Product '{}' not found for date {}. Skipping this item.", item.getProductName(), date);
        }
        
        return productsByName;
    }

    /**
     * Create store shopping lists with cost breakdown
     */
    private List<ShoppingList> createStoreShoppingLists(Map<String, List<ProductSelection>> storeSelections) {
        return storeSelections.entrySet().stream()
                .map(entry -> {
                    String store = entry.getKey();
                    List<ProductSelection> selections = entry.getValue();
                    
                    List<Product> products = new ArrayList<>();
                    BigDecimal storeOriginalCost = BigDecimal.ZERO;
                    BigDecimal storeCostAfterDiscounts = BigDecimal.ZERO;
                    
                    for (ProductSelection selection : selections) {
                        // Create product with effective price for response
                        Product product = createDiscountedProduct(selection.productWithDiscount());
                        products.add(product);
                        
                        // Calculate costs
                        BigDecimal originalCost = selection.productWithDiscount().getProduct().getPrice()
                                .multiply(BigDecimal.valueOf(selection.quantity()));
                        BigDecimal effectiveCost = selection.getTotalCost();
                        
                        storeOriginalCost = storeOriginalCost.add(originalCost);
                        storeCostAfterDiscounts = storeCostAfterDiscounts.add(effectiveCost);
                    }
                    
                    BigDecimal storeSavings = storeOriginalCost.subtract(storeCostAfterDiscounts);
                    
                    return ShoppingList.builder()
                            .storeName(store)
                            .products(products)
                            .itemCount(products.size())
                            .originalCost(storeOriginalCost)
                            .costAfterDiscounts(storeCostAfterDiscounts)
                            .savings(storeSavings)
                            .build();
                })
                .sorted(Comparator.comparing(ShoppingList::getStoreName))
                .collect(Collectors.toList());
    }

    /**
     * Create a product with the effective (discounted) price
     */
    private Product createDiscountedProduct(ProductWithDiscount pwd) {
        Product original = pwd.getProduct();
        return Product.builder()
                .id(original.getId())
                .productId(original.getProductId())
                .name(original.getName())
                .category(original.getCategory())
                .brand(original.getBrand())
                .packageQuantity(original.getPackageQuantity())
                .packageUnit(original.getPackageUnit())
                .price(pwd.getEffectivePrice()) // Use discounted price
                .currency(original.getCurrency())
                .store(original.getStore())
                .priceDate(original.getPriceDate())
                .build();
    }
}
