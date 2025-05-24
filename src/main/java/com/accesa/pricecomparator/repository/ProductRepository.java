package com.accesa.pricecomparator.repository;

import com.accesa.pricecomparator.model.Product;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;

@Repository
public interface ProductRepository extends JpaRepository<Product, Long> {

    List<Product> findByPriceDate(LocalDate date);

    List<Product> findByStoreAndPriceDate(String store, LocalDate priceDate);

    List<Product> findByProductIdAndStoreAndPriceDate(String productId, String store, LocalDate priceDate);

    List<Product> findByName(String name);

    List<Product> findByNameAndPriceDate(String name, LocalDate priceDate);

    List<Product> findByNameAndBrand(String name, String brand);

    List<Product> findByNameAndCategory(String name, String category);

    List<Product> findByNameAndPriceDateBetweenOrderByPriceDateAsc(
            String name,
            LocalDate startDate,
            LocalDate endDate
    );

    List<Product> findByNameAndStoreAndPriceDateBetweenOrderByPriceDateAsc(
            String name,
            String store,
            LocalDate startDate,
            LocalDate endDate
    );

    List<Product> findByBrandAndPriceDateBetweenOrderByPriceDateAsc(
            String brand,
            LocalDate startDate,
            LocalDate endDate
    );

    List<Product> findByBrandAndStoreAndPriceDateBetweenOrderByPriceDateAsc(
            String brand,
            String store,
            LocalDate startDate,
            LocalDate endDate
    );

    List<Product> findByCategoryAndPriceDateBetweenOrderByPriceDateAsc(
            String category,
            LocalDate startDate,
            LocalDate endDate
    );

    List<Product> findByCategoryAndStoreAndPriceDateBetweenOrderByPriceDateAsc(
            String category,
            String store,
            LocalDate startDate,
            LocalDate endDate
    );
}
