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
        
        // Get current discounts for today
        Map<String, Map<String, Discount>> discountMap = buildDiscountMap(today);
        
        for (Alert alert : activeAlerts) {
            // Find the latest available product data for this specific product/store combination
            var allProductVersions = productRepository.findByNameAndStoreAndPriceDateBetweenOrderByPriceDateAsc(
                    alert.getProductName(),
                    alert.getStore(),
                    LocalDate.now().minusMonths(1), // Start from a reasonable past date
                    today
            );

            if (allProductVersions.isEmpty()) {
                log.debug("No products found for alert: {} - {}",
                        alert.getProductName(),
                        alert.getStore()
                );
                continue;
            }

            // Get the latest product data for this specific product/store
            Product product = allProductVersions.getLast();

            // Get an applicable discount for this product and store
            Discount applicableDiscount = findApplicableDiscount(product, discountMap);

            // Calculate effective price (considering discounts)
            ProductWithDiscount productWithDiscount = new ProductWithDiscount(product, applicableDiscount);
            BigDecimal effectivePrice = productWithDiscount.getEffectivePrice();

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
                        .currentPrice(effectivePrice)  // Use effective price instead of original price
                        .currency(product.getCurrency())
                        .message(message)
                        .build());

                // Deactivate the alert to prevent repeated notifications
                alert.setActive(false);
                alertRepository.save(alert);

                log.info("Price alert with ID {} has been deactivated after triggering", alert.getId());
            }
        }
        
        log.info("Price alert check completed. Found {} triggered alerts", triggeredAlerts.size());
        return triggeredAlerts;
    }

    private Map<String, Map<String, Discount>> buildDiscountMap(LocalDate date) {
        return discountRepository.findActiveOnDate(date).stream()
                .collect(Collectors.groupingBy(
                        Discount::getProductName,
                        Collectors.toMap(
                                Discount::getStore,
                                discount -> discount,
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
