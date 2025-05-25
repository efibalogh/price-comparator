package com.accesa.pricecomparator.repository;

import com.accesa.pricecomparator.model.Alert;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface AlertRepository extends JpaRepository<Alert, Long> {

    List<Alert> findByActiveTrue();

    List<Alert> findByProductNameAndStoreAndActiveTrue(String productName, String store);
}
