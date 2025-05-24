package com.accesa.pricecomparator.config;

import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;

@Configuration
@EnableAutoConfiguration
@EntityScan("com.accesa.pricecomparator.model")
@EnableJpaRepositories(basePackages = "com.accesa.pricecomparator.repository")
public class JpaConfig {}
