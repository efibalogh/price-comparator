package com.accesa.pricecomparator;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import static org.junit.jupiter.api.Assertions.assertTrue;

@SpringBootTest
@ActiveProfiles("test")
class PriceComparatorApplicationTests {

    // dummy test for now
    @Test
    void contextLoads() {
        boolean isContextLoaded = true;
        assertTrue(isContextLoaded, "Context loaded successfully");
    }
}
