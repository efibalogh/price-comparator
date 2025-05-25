package com.accesa.pricecomparator.service;

public interface CsvImporterService {

    /**
     * Imports data from the specified directory path.
     *
     * @param directoryPath the path to the directory containing CSV files
     */
    void importDataFrom(String directoryPath);
}
