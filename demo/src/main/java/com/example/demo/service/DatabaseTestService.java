package com.example.demo.service;



import com.example.demo.model.LoadTestRequest;

import java.util.List;

/**
 * Service interface for performing database stress/load testing operations.
 * <p>
 * Defines methods for writing, reading, updating, and loading base documents
 * as well as inserting initial data needed for tests.
 */
public interface DatabaseTestService {

    /**
     * Performs a concurrent write (insert) test using the given request configuration.
     *
     * @param request LoadTestRequest containing insert count and thread configuration
     */
    void writeTest(LoadTestRequest request);

    /**
     * Performs a concurrent read test using the given request configuration.
     *
     * @param request LoadTestRequest containing read count and thread configuration
     */
    void readTest(LoadTestRequest request);

    /**
     * Loads base documents from provided JSON files on the classpath
     * into an in-memory list for use during insert operations.
     *
     * @param files List of JSON file names to load
     */
    void loadBaseDocuments(List<String> files);

    /**
     * Performs a concurrent update test using the given request configuration.
     *
     * @param request LoadTestRequest containing update count and thread configuration
     */
    void updateTest(LoadTestRequest request);

    /**
     * Inserts initial data required for read and update tests,
     * using pre-loaded base documents.
     */
    void insertDataForReadAndUpdate();
}
