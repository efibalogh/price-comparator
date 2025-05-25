package com.accesa.pricecomparator.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serial;
import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDate;

@Data
@Builder
@Entity
@Table(name = "price_alerts")
@NoArgsConstructor
@AllArgsConstructor
public class Alert implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "product_name", nullable = false)
    private String productName;

    @Column(nullable = false)
    private String store;

    @Column(nullable = false)
    private BigDecimal targetPrice;

    @Column(nullable = false)
    private boolean active;

    @Column(nullable = false)
    private LocalDate creationDate;
}
