package com.accesa.pricecomparator.service.impl;

import com.accesa.pricecomparator.dto.response.TriggeredAlertResponse;
import com.accesa.pricecomparator.exception.ResourceNotFoundException;
import com.accesa.pricecomparator.model.Discount;
import com.accesa.pricecomparator.model.Alert;
import com.accesa.pricecomparator.model.Product;
import com.accesa.pricecomparator.model.ProductWithDiscount;
import com.accesa.pricecomparator.repository.DiscountRepository;
import com.accesa.pricecomparator.repository.AlertRepository;
import com.accesa.pricecomparator.repository.ProductRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class AlertServiceImpl implements com.accesa.pricecomparator.service.AlertService {

    private final AlertRepository alertRepository;
    private final ProductRepository productRepository;
    private final DiscountRepository discountRepository;

    @Override
    @Transactional
    public Alert create(String productName, String store, BigDecimal targetPrice) {
        Alert alert = Alert.builder()
                .productName(productName)
                .store(store)
                .targetPrice(targetPrice)
                .active(true)
                .creationDate(LocalDate.now())
                .build();
        log.info("Created price alert for product '{}' in store '{}' at target price {}", 
                productName, store, targetPrice);
        return alertRepository.save(alert);
    }

    @Override
    @Transactional
    public void activate(Long id) {
        Alert alert = alertRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Price alert not found with ID: " + id));
        alert.setActive(true);
        alertRepository.save(alert);
        log.info("Activated price alert with ID: {}", id);
    }

    @Override
    @Transactional
    public void deactivate(Long id) {
        Alert alert = alertRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Price alert not found with ID: " + id));
        alert.setActive(false);
        alertRepository.save(alert);
        log.info("Deactivated price alert with ID: {}", id);
    }

    /**
     * Comprehensive alert checking algorithm that processes all active alerts against current prices.
     * Algorithm Details:
     * 1. Retrieves all active alerts from the database
     * 2. Builds a discount lookup map for efficient discount resolution
     * 3. For each alert, finds the most recent product data within a reasonable timeframe
     * 4. Applies any applicable discounts to calculate the effective price
     * 5. Compares effective price against the alert's target price
     * 6. Triggers alerts when effective price <= target price
     * 7. Automatically deactivates triggered alerts to prevent spam
     * Important Notes:
     * - Uses a 1-month lookback period to find recent product data
     * - Considers discounts in price comparisons (alerts trigger on effective price, not original price)
     * - Alerts are automatically deactivated after triggering (one-time notification model)
     */
    @Override
    @Transactional
    public List<TriggeredAlertResponse> checkAllAlertsForLatestPrices() {
        log.info("Checking all active price alerts against latest product prices...");
        
        List<TriggeredAlertResponse> triggeredAlerts = new ArrayList<>();
        List<Alert> activeAlerts = alertRepository.findByActiveTrue();
        LocalDate today = LocalDate.now();
        
        if (activeAlerts.isEmpty()) {
            log.info("No active alerts found");
            return triggeredAlerts;
        }
        
        log.info("Checking {} active alerts with discounts for date: {}", 
                activeAlerts.size(),
                today
        );
        
        // Pre-build discount map for efficient lookup during alert processing
        Map<String, Map<String, Discount>> discountMap = buildDiscountMap(today);
        
        for (Alert alert : activeAlerts) {
            // Strategy: Look for the most recent product data within a reasonable timeframe
            // This handles cases where exact date matches might not exist
            var allProductVersions = productRepository.findByNameAndStoreAndPriceDateBetweenOrderByPriceDateAsc(
                    alert.getProductName(),
                    alert.getStore(),
                    LocalDate.now().minusMonths(1), // 1-month lookback window
                    today
            );

            if (allProductVersions.isEmpty()) {
                log.debug("No products found for alert: {} - {}",
                        alert.getProductName(),
                        alert.getStore()
                );
                continue;
            }

            // Use the most recent product data (last in the chronologically ordered list)
            Product product = allProductVersions.getLast();

            // Apply any applicable discount to get the effective price
            Discount applicableDiscount = findApplicableDiscount(product, discountMap);
            ProductWithDiscount productWithDiscount = new ProductWithDiscount(product, applicableDiscount);
            BigDecimal effectivePrice = productWithDiscount.getEffectivePrice();

            // Alert triggers when the effective price is at or below the target price
            if (effectivePrice.compareTo(alert.getTargetPrice()) <= 0) {
                log.info("ALERT TRIGGERED! Product: {} at {}, Current: {} (Original: {}), Target: {}, Product Date: {}",
                        product.getName(),
                        product.getStore(),
                        effectivePrice,
                        product.getPrice(),
                        alert.getTargetPrice(),
                        product.getPriceDate()
                );

                String message = buildAlertMessage(product, effectivePrice, applicableDiscount);

                triggeredAlerts.add(TriggeredAlertResponse.builder()
                        .productName(product.getName())
                        .store(product.getStore())
                        .targetPrice(alert.getTargetPrice())
                        .currentPrice(effectivePrice)  // Use effective price (after discount) in response
                        .currency(product.getCurrency())
                        .message(message)
                        .build());

                // Auto-deactivate to prevent repeated notifications
                // This implements a "one-shot" alert model
                alert.setActive(false);
                alertRepository.save(alert);

                log.info("Price alert with ID {} has been deactivated after triggering", alert.getId());
            }
        }
        
        log.info("Price alert check completed. Found {} triggered alerts", triggeredAlerts.size());
        return triggeredAlerts;
    }

    /**
     * Builds an efficient lookup structure for discounts.
     * Structure: productName -> storeName -> bestDiscount
     * Conflict Resolution Strategy:
     * When multiple discounts exist for the same product/store combination,
     * selects the discount with the highest percentage (most beneficial to customer).
     */
    private Map<String, Map<String, Discount>> buildDiscountMap(LocalDate date) {
        return discountRepository.findActiveOnDate(date).stream()
                .collect(Collectors.groupingBy(
                        Discount::getProductName,
                        Collectors.toMap(
                                Discount::getStore,
                                discount -> discount,
                                // Choose the discount with the higher percentage if multiple exist
                                (d1, d2) -> d1
                                        .getPercentageOfDiscount()
                                        .compareTo(d2.getPercentageOfDiscount()) > 0 ? d1 : d2
                        )
                ));
    }

    private Discount findApplicableDiscount(Product product, Map<String, Map<String, Discount>> discountMap) {
        if (discountMap.containsKey(product.getName())
            && discountMap.get(product.getName()).containsKey(product.getStore())) {
            return discountMap.get(product.getName()).get(product.getStore());
        }
        return null;
    }

    private String buildAlertMessage(Product product, BigDecimal effectivePrice, Discount applicableDiscount) {
        if (applicableDiscount != null) {
            return String.format(
                    "Price dropped to %s %s (Original: %s %s, Discount: %s%%)",
                    effectivePrice, product.getCurrency(),
                    product.getPrice(), product.getCurrency(),
                    applicableDiscount.getPercentageOfDiscount()
            );
        }
        return String.format("Price dropped to %s %s", effectivePrice, product.getCurrency());
    }
}
