package com.accesa.pricecomparator.service.impl;

import com.accesa.pricecomparator.dto.response.BestDiscountResponse;
import com.accesa.pricecomparator.model.Discount;
import com.accesa.pricecomparator.repository.DiscountRepository;
import com.accesa.pricecomparator.service.DiscountService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class DiscountServiceImpl implements DiscountService {

    @Autowired
    private final DiscountRepository discountRepository;

    @Override
    public List<Discount> getAll() {
        return discountRepository.findAll();
    }

    @Override
    public List<Discount> getCurrent(LocalDate date) {
        return discountRepository.findActiveOnDate(date);
    }

    @Override
    public List<BestDiscountResponse> getBest(LocalDate date, int limit) {
        return discountRepository.findBestDiscountsByProductForDate(date).stream()
                .map(discount -> new BestDiscountResponse(
                        discount.getProductName(),
                        discount.getBrand(),
                        discount.getStore(),
                        discount.getFromDate(),
                        discount.getToDate(),
                        discount.getPercentageOfDiscount()
                ))
                .limit(limit)
                .collect(Collectors.toList());
    }

    @Override
    public List<Discount> getNew(int daysBack) {
        LocalDate thresholdDate = LocalDate.now().minusDays(daysBack);
        return discountRepository.findByDiscountDateGreaterThanEqual(thresholdDate);
    }
}
