package com.accesa.pricecomparator.service;

import com.accesa.pricecomparator.dto.response.TriggeredAlertResponse;
import com.accesa.pricecomparator.model.Alert;

import java.math.BigDecimal;
import java.util.List;

public interface AlertService {

    /**
     * Creates a new price alert for a specific product and store with a target price.
     *
     * @param productName the name of the product
     * @param store       the store where the product is sold
     * @param targetPrice the target price for the alert
     * @return the created PriceAlert object
     */
    Alert create(String productName, String store, BigDecimal targetPrice);

    /**
     * Activates a price alert by its ID.
     *
     * @param id the ID of the price alert to activate
     */
    void activate(Long id);

    /**
     * Deactivates a price alert by its ID.
     *
     * @param id the ID of the price alert to deactivate
     */
    void deactivate(Long id);

    /**
     * Check all active price alerts against the latest product prices
     * This method is called after CSV import to check all alerts
     *
     * @return List of triggered alerts
     */
    List<TriggeredAlertResponse> checkAllAlertsForLatestPrices();
}
