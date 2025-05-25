package com.accesa.pricecomparator.service;

import com.accesa.pricecomparator.dto.response.PriceHistoryResponse;
import com.accesa.pricecomparator.dto.response.ValuePerUnitResponse;
import com.accesa.pricecomparator.exception.ResourceNotFoundException;
import com.accesa.pricecomparator.model.Product;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public interface ProductService {

    /**
     * Fetches all products from the repository.
     *
     * @return List of all products.
     */
    List<Product> getAll();

    /**
     * Fetches a product by its ID.
     *
     * @param id The ID of the product.
     * @return The product with the specified ID.
     * @throws ResourceNotFoundException if the product is not found.
     */
    Optional<Product> getById(Long id);

    /**
     * Fetches products by their name and date.
     *
     * @param name The name of the product.
     * @param date The date for which to fetch products.
     * @return List of products with the specified name and date.
     */
    List<Product> getByNameAndDate(String name, LocalDate date);

    /**
     * Fetches products by their store and date.
     *
     * @param store The store name.
     * @param date  The date for which to fetch products.
     * @return List of products available in the specified store on the given date.
     */
    List<Product> getByStoreAndDate(String store, LocalDate date);

    /**
     * Calculates "value per unit" for products, useful for comparison across different package sizes.
     *
     * @param date The date for which to fetch product prices.
     * @return List of ProductValuePerUnitDto.
     */
    List<ValuePerUnitResponse> getValuePerUnit(LocalDate date);

    /**
     * Provides data points for price trends over time.
     * Filterable by store, product category, or brand.
     *
     * @param filter "name", "category", or "brand"
     * @param value The value of the filter
     * @param store Optional: Filter by store
     * @param startDate Optional: Start date for the history
     * @param endDate Optional: End date for the history
     * @return List of ProductPriceHistoryDto
     */
    List<PriceHistoryResponse> getPriceHistory(
            String filter,
            String value,
            String store,
            LocalDate startDate,
            LocalDate endDate
    );
}
