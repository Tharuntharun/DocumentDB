package com.example.demo.controller;


import com.example.demo.model.LoadTestRequest;
import com.example.demo.service.DatabaseTestService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.concurrent.CompletableFuture;

/**
 * REST controller for database stress/load testing operations.
 * <p>
 * Provides endpoints to:
 * <ul>
 *   <li>Insert initial data into the database for read and update tests</li>
 *   <li>Run a series of concurrent load tests involving read, write, and update operations</li>
 * </ul>
 */
@RestController
@RequestMapping("/db-test")
public class DatabaseTestController {

    private final DatabaseTestService databaseTestService;
    private static final Logger log = LoggerFactory.getLogger(DatabaseTestController.class);

    public DatabaseTestController(DatabaseTestService databaseTestService) {
        this.databaseTestService = databaseTestService;
    }

    /**
     * Endpoint to run a series of load tests involving concurrent read, write, and update operations.
     * <p>
     * The test runs through different configurations with varying thread counts and record counts.
     * Each configuration is executed sequentially, and each test iteration runs the three operations
     * (write, update, read) in parallel using separate threads.
     *
     * @return A message indicating that the load test has completed.
     * @throws InterruptedException if the thread is interrupted during execution.
     */
    @GetMapping("/run-test")
    public String runTest() throws InterruptedException {

        databaseTestService.insertDataForReadAndUpdate();
        // Load base documents used for further inserts
        databaseTestService.loadBaseDocuments(List.of("order_1.json"));
        long startTimeMillis = System.currentTimeMillis();

        // Run through multiple scenarios (0 to 5) with different configurations
        for (int i = 0; i <= 5; i++) {
            LoadTestRequest request = new LoadTestRequest();

            // Configure each iteration with different counts and thread settings
            switch (i) {
                case 0 -> {
                    request.setReadCount(1500);
                    request.setInsertCount(3000);
                    request.setUpdateCount(1500);
                    request.setThreadCounts(4);
                }
                case 1 -> {
                    request.setReadCount(2000);
                    request.setInsertCount(4000);
                    request.setUpdateCount(2000);
                    request.setThreadCounts(6);
                }
                case 2 -> {
                    request.setReadCount(2500);
                    request.setInsertCount(5000);
                    request.setUpdateCount(2500);
                    request.setThreadCounts(8);
                }
                case 3 -> {
                    request.setReadCount(5000);
                    request.setInsertCount(10000);
                    request.setUpdateCount(5000);
                    request.setThreadCounts(12);
                }
                case 4 -> {
                    request.setReadCount(10000);
                    request.setInsertCount(20000);
                    request.setUpdateCount(10000);
                    request.setThreadCounts(14);
                }
                case 5 -> {
                    request.setReadCount(25000);
                    request.setInsertCount(50000);
                    request.setUpdateCount(25000);
                    request.setThreadCounts(18);
                }
            }

            // Create asynchronous tasks for write, update, and read tests
            CompletableFuture<Void> writeFuture = CompletableFuture.runAsync(() -> {
                try {
                    databaseTestService.writeTest(request);
                } catch (Exception e) {
                    throw new RuntimeException("Write load test failed", e);
                }
            });

            CompletableFuture<Void> updateTest = CompletableFuture.runAsync(() -> {
                try {
                    databaseTestService.updateTest(request);
                } catch (Exception e) {
                    throw new RuntimeException("Read load test failed", e);
                }
            });

            CompletableFuture<Void> readFuture = CompletableFuture.runAsync(() -> {
                try {
                    databaseTestService.readTest(request);
                } catch (Exception e) {
                    throw new RuntimeException("Read load test failed", e);
                }
            });

            // Wait for all three tasks to complete before moving to next scenario
            CompletableFuture.allOf(writeFuture, updateTest, readFuture).join();
            long totalTime = System.currentTimeMillis() - startTimeMillis;
            log.info("All Operations completed in {} seconds", String.format("%.2f", totalTime / 1000.0));
        }
        long totalTime = System.currentTimeMillis() - startTimeMillis;
        log.info("total completion in {} seconds", String.format("%.2f", totalTime / 1000.0));
        return "Load test completed!";
    }

}

