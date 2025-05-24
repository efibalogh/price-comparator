package com.accesa.pricecomparator.repository;

import com.accesa.pricecomparator.model.Discount;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;

@Repository
public interface DiscountRepository extends JpaRepository<Discount, Long> {

    List<Discount> findByFromDateLessThanEqualAndToDateGreaterThanEqual(LocalDate from, LocalDate to);

    List<Discount> findByDiscountDate(LocalDate discountDate);

    List<Discount> findByStoreAndDiscountDate(String store, LocalDate discountDate);

    List<Discount> findByDiscountDateGreaterThanEqual(LocalDate date);

    @Query("""
        SELECT d FROM Discount d\s
        WHERE d.id IN (
            SELECT d2.id FROM Discount d2\s
            WHERE d2.fromDate <= :date\s
            AND d2.toDate >= :date\s
            AND d2.percentageOfDiscount = (
                SELECT MAX(d3.percentageOfDiscount)\s
                FROM Discount d3\s
                WHERE d3.productName = d2.productName\s
                AND d3.brand = d2.brand\s
                AND d3.fromDate <= :date\s
                AND d3.toDate >= :date
            )
        )
        ORDER BY d.percentageOfDiscount DESC
       \s""")
    List<Discount> findBestDiscountsByProductForDate(@Param("date") LocalDate date);
}
