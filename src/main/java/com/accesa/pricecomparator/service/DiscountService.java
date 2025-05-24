package com.accesa.pricecomparator.service;

import com.accesa.pricecomparator.dto.response.BestDiscountResponse;
import com.accesa.pricecomparator.model.Discount;

import java.time.LocalDate;
import java.util.List;

public interface DiscountService {

    /**
     * Retrieves all discounts.
     *
     * @return List of all discounts.
     */
    List<Discount> getAll();

    /**
     * Retrieves discounts for a specific date.
     *
     * @param date The date for which to retrieve discounts.
     * @return List of discounts for the specified date.
     */
    List<Discount> getCurrent(LocalDate date);

    /**
     * Lists products with the highest current percentage discounts across all tracked stores.
     *
     * @param date The date for which to check for current discounts.
     * @param limit The maximum number of discounts to return.
     * @return List of best discounts, ordered by percentage of discount descending.
     */
    List<BestDiscountResponse> getBest(LocalDate date, int limit);

    /**
     * Lists discounts that have been newly added (e.g., within the last 24 hours).
     * Assuming "newly added" means the `discountDate` in the CSV file was within the last `x` days.
     *
     * @param daysBack The number of days to look back for newly added discounts.
     * @return List of newly added discounts.
     */
    List<Discount> getNew(int daysBack);
}
