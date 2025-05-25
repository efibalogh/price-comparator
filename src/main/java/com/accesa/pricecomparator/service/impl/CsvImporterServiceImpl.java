package com.accesa.pricecomparator.service.impl;

import com.accesa.pricecomparator.model.Discount;
import com.accesa.pricecomparator.model.Product;
import com.accesa.pricecomparator.repository.DiscountRepository;
import com.accesa.pricecomparator.repository.ProductRepository;
import com.accesa.pricecomparator.service.CsvImporterService;
import com.opencsv.CSVParserBuilder;
import com.opencsv.CSVReader;
import com.opencsv.CSVReaderBuilder;
import com.opencsv.exceptions.CsvValidationException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.math.BigDecimal;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import static org.apache.commons.lang3.StringUtils.trim;

@Slf4j
@Service
@RequiredArgsConstructor
public class CsvImporterServiceImpl implements CsvImporterService {

    @Autowired
    private final ProductRepository productRepository;

    @Autowired
    private final DiscountRepository discountRepository;

    private static final String PRODUCT_FILE_SUFFIX = ".csv";
    private static final String DISCOUNT_FILE_INFIX = "_discounts_";
    private static final Pattern PRODUCT_FILENAME_PATTERN = Pattern.compile(
            "([a-zA-Z0-9]+)_(\\d{4}-\\d{2}-\\d{2})\\.csv"
    );
    private static final Pattern DISCOUNT_FILENAME_PATTERN = Pattern.compile(
            "([a-zA-Z0-9]+)_discounts_(\\d{4}-\\d{2}-\\d{2})\\.csv"
    );

    // Helper records for parsed filename information
    private record FileInfo(String storeName, LocalDate date) {}

    // Helper class for import statistics
    private static final class ImportCounters {
        int duplicatesSkipped;
        int updatedCount;
        int newCount;

        void incrementDuplicatesSkipped() {
            duplicatesSkipped++;
        }

        void incrementUpdatedCount() {
            updatedCount++;
        }

        void incrementNewCount() {
            newCount++;
        }
    }

    @Override
    @Transactional
    public void importDataFrom(String directoryPath) {
        Path dir = Paths.get(directoryPath);
        if (!Files.exists(dir) || !Files.isDirectory(dir)) {
            log.error("Directory not found or is not a directory: {}", directoryPath);
            return;
        }

        try (DirectoryStream<Path> stream = Files.newDirectoryStream(dir, "*.csv")) {
            for (Path filePath : stream) {
                String filename = filePath.getFileName().toString();
                if (filename.contains(DISCOUNT_FILE_INFIX)) {
                    importDiscountData(filePath);
                } else if (filename.endsWith(PRODUCT_FILE_SUFFIX)) { // Ensure it's a product file
                    importProductPriceData(filePath);
                } else {
                    log.warn("Skipping unrecognized file: {}", filename);
                }
            }
        } catch (IOException e) {
            log.error("Error reading CSV files from directory {}: {}", directoryPath, e.getMessage());
        }
    }

    private Optional<FileInfo> parseFileName(String filename, Pattern pattern, String fileType) {
        Matcher matcher = pattern.matcher(filename);
        if (!matcher.matches()) {
            log.warn("Skipping file {}: Does not match {} filename pattern.", filename, fileType);
            return Optional.empty();
        }
        String storeName = matcher.group(1);
        try {
            LocalDate date = LocalDate.parse(matcher.group(2));
            return Optional.of(new FileInfo(storeName, date));
        } catch (DateTimeParseException e) {
            log.error("Invalid date format in {} filename {}: {}", fileType, filename, e.getMessage());
            return Optional.empty();
        }
    }

    private CSVReader createCsvReader(Path filePath) throws IOException {
        return new CSVReaderBuilder(Files.newBufferedReader(filePath))
                .withCSVParser(new CSVParserBuilder().withSeparator(';').build())
                .build();
    }

    private String normalizePackageUnit(String packageUnit) {
        if ("role".equalsIgnoreCase(trim(packageUnit))) {
            return "buc";
        }
        return trim(packageUnit);
    }

    // --- Product Import Methods ---

    private void importProductPriceData(Path filePath) {
        String filename = filePath.getFileName().toString();
        log.info("Importing product price data from: {}", filename);

        Optional<FileInfo> fileInfoOpt = parseFileName(filename, PRODUCT_FILENAME_PATTERN, "product price");
        if (fileInfoOpt.isEmpty()) {
            return;
        }
        FileInfo fileInfo = fileInfoOpt.get();

        Map<String, Product> existingProductMap = productRepository
                .findByStoreAndPriceDate(fileInfo.storeName(), fileInfo.date())
                .stream()
                .collect(Collectors.toMap(Product::getProductId, product -> product));

        List<Product> productsToSave = new ArrayList<>();
        Set<String> processedProductIdsInFile = new HashSet<>();
        ImportCounters counters = new ImportCounters();

        try (CSVReader reader = createCsvReader(filePath)) {
            reader.readNext(); // Skip header
            String[] line;
            while (true) {
                line = reader.readNext();
                if (line == null) {
                    break; // EOF
                }
                processProductLine(
                        line,
                        fileInfo,
                        existingProductMap,
                        processedProductIdsInFile,
                        productsToSave,
                        counters,
                        filename
                );
            }
            saveProductsAndLog(productsToSave, filename, counters);
        } catch (FileNotFoundException e) {
            log.error("File {} not found: {}", filename, e.getMessage());
        } catch (IOException | CsvValidationException e) {
            log.error("Error reading CSV file {}: {}", filename, e.getMessage());
        }
    }

    private void processProductLine(
            String[] line,
            FileInfo fileInfo,
            Map<String, Product> existingProductMap,
            Set<String> processedProductIdsInFile,
            List<Product> productsToSave,
            ImportCounters counters,
            String filename
    ) {
        if (line.length < 8) {
            log.warn("Skipping malformed product line in {}: {}", filename, String.join(";", line));
            return;
        }

        String productId = trim(line[0]);
        if (processedProductIdsInFile.contains(productId)) {
            log.debug("Skipping duplicate product ID {} in {}", productId, filename);
            counters.incrementDuplicatesSkipped();
            return;
        }
        processedProductIdsInFile.add(productId);

        try {
            Product product;
            if (existingProductMap.containsKey(productId)) {
                product = existingProductMap.get(productId);
                updateProductFromCsvLine(product, line);
                counters.incrementUpdatedCount();
            } else {
                product = createProductFromCsvLine(
                        line,
                        fileInfo.storeName(),
                        fileInfo.date()
                );
                counters.incrementNewCount();
            }
            productsToSave.add(product);
        } catch (NumberFormatException | DateTimeParseException e) {
            log.error(
                    "Error parsing data for product ID {} in {}: {} - {}",
                    productId,
                    filename,
                    String.join(";", line),
                    e.getMessage()
            );
        }
    }

    private Product createProductFromCsvLine(String[] line, String storeName, LocalDate priceDate) {
        return Product
                .builder()
                .productId(trim(line[0]))
                .name(trim(line[1]))
                .category(trim(line[2]))
                .brand(trim(line[3]))
                .packageQuantity(new BigDecimal(trim(line[4])))
                .packageUnit(normalizePackageUnit(line[5]))
                .price(new BigDecimal(trim(line[6])))
                .currency(trim(line[7]))
                .store(storeName)
                .priceDate(priceDate)
                .build();
    }

    private void updateProductFromCsvLine(Product product, String... line) {
        product.setName(trim(line[1]));
        product.setCategory(trim(line[2]));
        product.setBrand(trim(line[3]));
        product.setPackageQuantity(new BigDecimal(trim(line[4])));
        product.setPackageUnit(normalizePackageUnit(line[5]));
        product.setPrice(new BigDecimal(trim(line[6])));
        product.setCurrency(trim(line[7]));
    }

    private void saveProductsAndLog(List<Product> productsToSave, String filename, ImportCounters counters) {
        if (!productsToSave.isEmpty()) {
            productRepository.saveAll(productsToSave);
            log.info(
                    "Successfully imported {} products from {} ({} new, {} updated, {} duplicates skipped)",
                    productsToSave.size(),
                    filename,
                    counters.newCount,
                    counters.updatedCount,
                    counters.duplicatesSkipped
            );
            return;
        }
        log.info(
                "No products to import from {} (New: {}, Updated: {}, Duplicates Skipped: {}).",
                filename,
                counters.newCount,
                counters.updatedCount,
                counters.duplicatesSkipped
        );
    }

    // --- Discount Import Methods ---

    private void importDiscountData(Path filePath) {
        String filename = filePath.getFileName().toString();
        log.info("Importing discount data from: {}", filename);

        Optional<FileInfo> fileInfoOpt = parseFileName(filename, DISCOUNT_FILENAME_PATTERN, "discount");
        if (fileInfoOpt.isEmpty()) {
            return;
        }
        FileInfo fileInfo = fileInfoOpt.get();

        Map<String, Discount> existingDiscountMap = discountRepository
                .findByStoreAndDiscountDate(fileInfo.storeName(), fileInfo.date())
                .stream()
                .collect(Collectors.toMap(Discount::getProductId, discount -> discount));

        List<Discount> discountsToSave = new ArrayList<>();
        Set<String> processedProductIdsInFile = new HashSet<>();
        ImportCounters counters = new ImportCounters();

        try (CSVReader reader = createCsvReader(filePath)) {
            reader.readNext(); // Skip header
            String[] line;
            while (true) {
                line = reader.readNext();
                if (line == null) {
                    break; // EOF
                }
                processDiscountLine(
                        line,
                        fileInfo,
                        existingDiscountMap,
                        processedProductIdsInFile,
                        discountsToSave,
                        counters,
                        filename
                );
            }
            saveDiscountsAndLog(discountsToSave, filename, counters);
        } catch (FileNotFoundException e) {
            log.error("File {} not found: {}", filename, e.getMessage());
        } catch (IOException | CsvValidationException e) {
            log.error("Error reading CSV file {}: {}", filename, e.getMessage());
        }
    }

    private void processDiscountLine(
            String[] line,
            FileInfo fileInfo,
            Map<String, Discount> existingDiscountMap,
            Set<String> processedProductIdsInFile,
            List<Discount> discountsToSave,
            ImportCounters counters,
            String filename
    ) {
        if (line.length < 9) {
            log.warn("Skipping malformed discount line in {}: {}", filename, String.join(";", line));
            return;
        }

        String productId = trim(line[0]);
        if (processedProductIdsInFile.contains(productId)) {
            log.debug("Skipping duplicate discount for product ID {} in {}", productId, filename);
            counters.incrementDuplicatesSkipped();
            return;
        }
        processedProductIdsInFile.add(productId);

        try {
            Discount discount;
            if (existingDiscountMap.containsKey(productId)) {
                discount = existingDiscountMap.get(productId);
                updateDiscountFromCsvLine(discount, line);
                counters.incrementUpdatedCount();
            } else {
                discount = createDiscountFromCsvLine(line, fileInfo.storeName(), fileInfo.date());
                counters.incrementNewCount();
            }
            discountsToSave.add(discount);
        } catch (NumberFormatException | DateTimeParseException e) {
            log.error(
                    "Error parsing data for discount on product ID {} in {}: {} - {}",
                    productId,
                    filename,
                    String.join(";", line),
                    e.getMessage()
            );
        }
    }


    private Discount createDiscountFromCsvLine(String[] line, String storeName, LocalDate discountFileDate) {
        return Discount
                .builder()
                .productId(trim(line[0]))
                .productName(trim(line[1]))
                .brand(trim(line[2]))
                .packageQuantity(new BigDecimal(trim(line[3])))
                .packageUnit(normalizePackageUnit(line[4]))
                .productCategory(trim(line[5]))
                .fromDate(LocalDate.parse(trim(line[6])))
                .toDate(LocalDate.parse(trim(line[7])))
                .percentageOfDiscount(new BigDecimal(trim(line[8])))
                .store(storeName)
                .discountDate(discountFileDate) // This is the date from the filename
                .build();
    }

    private void updateDiscountFromCsvLine(Discount discount, String... line) {
        discount.setProductName(trim(line[1]));
        discount.setBrand(trim(line[2]));
        discount.setPackageQuantity(new BigDecimal(trim(line[3])));
        discount.setPackageUnit(normalizePackageUnit(line[4]));
        discount.setProductCategory(trim(line[5]));
        discount.setFromDate(LocalDate.parse(trim(line[6])));
        discount.setToDate(LocalDate.parse(trim(line[7])));
        discount.setPercentageOfDiscount(new BigDecimal(trim(line[8])));
        // store and discountDate (from filename) are not updated as they define the existing record's context
    }

    private void saveDiscountsAndLog(List<Discount> discountsToSave, String filename, ImportCounters counters) {
        if (!discountsToSave.isEmpty()) {
            discountRepository.saveAll(discountsToSave);
            log.info(
                    "Successfully imported {} discounts from {} ({} new, {} updated, {} duplicates skipped)",
                    discountsToSave.size(),
                    filename,
                    counters.newCount,
                    counters.updatedCount,
                    counters.duplicatesSkipped
            );
            return;
        }
        log.info(
                "No discounts to import from {} (New: {}, Updated: {}, Duplicates Skipped: {}).",
                filename,
                counters.newCount,
                counters.updatedCount,
                counters.duplicatesSkipped
        );
    }
}
