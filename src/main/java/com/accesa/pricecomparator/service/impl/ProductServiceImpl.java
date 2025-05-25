package com.accesa.pricecomparator.service.impl;

import com.accesa.pricecomparator.dto.response.PriceHistoryResponse;
import com.accesa.pricecomparator.dto.response.ValuePerUnitResponse;
import com.accesa.pricecomparator.exception.ResourceNotFoundException;
import com.accesa.pricecomparator.model.Product;
import com.accesa.pricecomparator.repository.ProductRepository;
import com.accesa.pricecomparator.service.ProductService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.orm.jpa.JpaSystemException;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.*;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class ProductServiceImpl implements ProductService {

    private static final Set<String> VALID_TYPES = Set.of("name", "category", "brand");

    private final ProductRepository productRepository;

    @Override
    public List<Product> getAll() {
        return productRepository.findAll();
    }

    @Override
    public Optional<Product> getById(Long id) {
        try {
            return productRepository.findById(id);
        } catch (JpaSystemException e) {
            log.error("Error fetching product with ID {}: {}", id, e.getMessage());
            throw new ResourceNotFoundException("Product not found with id: " + id);
        }
    }

    @Override
    public List<Product> getByNameAndDate(String name, LocalDate date) {
        return productRepository.findByNameAndPriceDate(name, date);
    }

    @Override
    public List<Product> getByStoreAndDate(String store, LocalDate date) {
        return productRepository.findByStoreAndPriceDate(store, date);
    }

    @Override
    public List<ValuePerUnitResponse> getValuePerUnit(LocalDate date) {
        List<Product> products = productRepository.findByPriceDate(date);
        List<ValuePerUnitResponse> valuePerUnitList = new ArrayList<>();

        for (Product product : products) {
            BigDecimal valuePerUnit;
            if (product.getPackageQuantity() != null && product.getPackageQuantity().compareTo(BigDecimal.ZERO) > 0) {
                valuePerUnit = product.getPrice().divide(product.getPackageQuantity(), 4, RoundingMode.HALF_UP);
            } else {
                log.warn(
                        "Product {} in store {} has invalid package quantity {}. Cannot calculate value per unit.",
                        product.getName(),
                        product.getStore(),
                        product.getPackageQuantity()
                );
                continue;
            }
            valuePerUnitList.add(
                    ValuePerUnitResponse.builder()
                            .productName(product.getName())
                            .brand(product.getBrand())
                            .store(product.getStore())
                            .price(product.getPrice())
                            .packageQuantity(product.getPackageQuantity())
                            .packageUnit(product.getPackageUnit())
                            .valuePerUnit(valuePerUnit)
                            .currency(product.getCurrency())
                            .build()
            );
        }
        // Sort by product name and then by value per unit for easier comparison
        valuePerUnitList
                .sort(Comparator.comparing(ValuePerUnitResponse::getProductName)
                        .thenComparing(ValuePerUnitResponse::getValuePerUnit));
        return valuePerUnitList;
    }

    @Override
    public List<PriceHistoryResponse> getPriceHistory(
            String filter,
            String value,
            String store,
            LocalDate startDate,
            LocalDate endDate
    ) {
        String normalizedFilter = filter.toLowerCase(Locale.getDefault());
        if (!VALID_TYPES.contains(normalizedFilter)) {
            log.warn("Invalid product history type provided: {}", filter);
            throw new IllegalArgumentException("Invalid identifier type. Must be 'name', 'category', or 'brand'.");
        }
        // If the startDate is missing, set it to a year ago
        final LocalDate start = Objects.requireNonNullElseGet(startDate, () -> LocalDate.now().minusYears(1));
        // If the endDate is missing, set it to a year from now
        final LocalDate end = Objects.requireNonNullElseGet(endDate, () -> LocalDate.now().plusYears(1));

        List<Product> products = fetchProductsForHistory(
                normalizedFilter,
                value,
                store,
                start,
                end
        );

        return groupProductsForHistory(products);
    }

    private List<Product> fetchProductsForHistory(
            String filter,
            String value,
            String store,
            LocalDate start,
            LocalDate end
    ) {
        if (store != null) {
            return fetchProductsByStoreAndFilter(filter, value, store, start, end);
        }
        return fetchProductsByFilter(filter, value, start, end);
    }

    private List<Product> fetchProductsByStoreAndFilter(
            String filter,
            String value,
            String store,
            LocalDate start,
            LocalDate end
    ) {
        return switch (filter) {
            case "name" -> productRepository.findByNameAndStoreAndPriceDateBetweenOrderByPriceDateAsc(
                    value,
                    store,
                    start,
                    end
            );
            case "category" -> productRepository.findByCategoryAndStoreAndPriceDateBetweenOrderByPriceDateAsc(
                    value,
                    store,
                    start,
                    end
            );
            case "brand" -> productRepository.findByBrandAndStoreAndPriceDateBetweenOrderByPriceDateAsc(
                    value,
                    store,
                    start,
                    end
            );
            default -> {
                // Should not be reached due to pre-validation
                log.error("Invalid filter '{}' in fetchProductsByStoreAndType after validation.", filter);
                throw new IllegalStateException("Invalid filter: " + filter);
            }
        };
    }

    private List<Product> fetchProductsByFilter(
            String filter,
            String value,
            LocalDate start,
            LocalDate end
    ) {
        return switch (filter) {
            case "name" -> productRepository.findByNameAndPriceDateBetweenOrderByPriceDateAsc(
                    value,
                    start,
                    end
            );
            case "category" -> productRepository.findByCategoryAndPriceDateBetweenOrderByPriceDateAsc(
                    value,
                    start,
                    end
            );
            case "brand" -> productRepository.findByBrandAndPriceDateBetweenOrderByPriceDateAsc(
                    value,
                    start,
                    end
            );
            default -> {
                // Should not be reached due to pre-validation
                log.error("Invalid filter '{}' in fetchProductsByType after validation.", filter);
                throw new IllegalStateException("Invalid filter: " + filter);
            }
        };
    }

    /**
     * Groups products by name and store and collects their price history.
     *
     * @param products List of products to group.
     * @return List of PriceHistoryResponse objects.
     */
    private List<PriceHistoryResponse> groupProductsForHistory(List<Product> products) {
        Map<String, Map<String, List<PriceHistoryResponse.PricePoint>>> historyMap = products.stream()
                .collect(Collectors.groupingBy(
                        Product::getName,
                        Collectors.groupingBy(
                                Product::getStore,
                                Collectors.mapping(
                                        p -> new PriceHistoryResponse.PricePoint(p.getPriceDate(), p.getPrice()),
                                        Collectors.toList()
                                )
                        )
                ));

        List<PriceHistoryResponse> result = new ArrayList<>();
        historyMap.forEach((product, storeMap) ->
                storeMap.forEach((s, pricePoints) ->
                        result.add(new PriceHistoryResponse(product, s, pricePoints))
                )
        );
        return result;
    }
}
