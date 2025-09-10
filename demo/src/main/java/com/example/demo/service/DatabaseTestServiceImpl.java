package com.example.demo.service;

import com.example.demo.model.LoadTestRequest;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.mongodb.client.MongoClient;
import com.mongodb.client.MongoClients;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoDatabase;
import com.mongodb.client.model.Filters;
import com.mongodb.client.model.Updates;
import org.bson.Document;
import org.bson.conversions.Bson;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Service;

import java.io.*;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadLocalRandom;

/**
 * Service implementation for database stress/load testing operations.
 * <p>
 * Provides methods to:
 * <ul>
 *   <li>Load base documents from JSON files</li>
 *   <li>Insert (write) test data concurrently</li>
 *   <li>Perform concurrent read operations</li>
 *   <li>Perform concurrent update operations</li>
 *   <li>Insert initial data required for read and update tests</li>
 * </ul>
 */
@Service
public class DatabaseTestServiceImpl implements DatabaseTestService {

    private static final Logger log = LoggerFactory.getLogger(DatabaseTestServiceImpl.class);

    MongoCollection<Document> numbersCollection;
    private List<Document> documents;
    private final List<Document> baseDocuments = new ArrayList<>();
    @Value("${connection.string}")
    String connectionString;

    public DatabaseTestServiceImpl() {

        ClassPathResource resource = new ClassPathResource("rds-truststore.jks");

        // Create a temp file
        File tempFile = null;
        try {
            tempFile = File.createTempFile("truststore", ".jks");
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        tempFile.deleteOnExit(); // delete on JVM exit

        // Copy contents from resource to temp file
        try (InputStream in = resource.getInputStream();
             OutputStream out = new FileOutputStream(tempFile)) {
            byte[] buffer = new byte[1024];
            int length;
            while ((length = in.read(buffer)) > 0) {
                out.write(buffer, 0, length);
            }
        } catch (FileNotFoundException e) {
            throw new RuntimeException(e);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

        // Set system properties
        System.setProperty("javax.net.ssl.trustStore", tempFile.getAbsolutePath());
        System.setProperty("javax.net.ssl.trustStorePassword", "truststore");

    }

    /**
     * Loads base documents from provided JSON files on the classpath.
     * These documents are used for subsequent insert (write) tests.
     *
     * @param files List of file names to load from classpath
     */
    @Override
    public void loadBaseDocuments(List<String> files) {
        ObjectMapper mapper = new ObjectMapper();
        if (baseDocuments.isEmpty() && !files.isEmpty()) {
            for (String file : files) {
                ClassPathResource resource = new ClassPathResource(file);

                try (InputStream is = resource.getInputStream()) {
                    List<Object> list = mapper.readValue(is, new TypeReference<List<Object>>() {
                    });
                    this.documents = list.stream()
                            .map(obj -> {
                                try {
                                    return Document.parse(mapper.writeValueAsString(obj));
                                } catch (JsonProcessingException e) {
                                    throw new RuntimeException(e);
                                }
                            })
                            .toList();
                    baseDocuments.addAll(documents);
                    log.info("Loaded " + baseDocuments.size() + " base documents from JSON.");
                } catch (Exception e) {
                    throw new RuntimeException("Failed to load file: " + file, e);
                }
            }
        }
    }

    /**
     * Performs a concurrent write test, inserting documents into the database
     * using multiple threads based on the provided request configuration.
     *
     * @param request LoadTestRequest containing insert count and thread configuration
     */
    @Override
    public void writeTest(LoadTestRequest request) {
        if (baseDocuments.isEmpty()) {
            throw new IllegalStateException("Base documents are not loaded!");
        }

        ExecutorService executor = Executors.newFixedThreadPool(request.getThreadCounts());
//        MongoDatabase db = mongoTemplate.getDb();
//        MongoCollection<Document> collection = db.getCollection("testCollection");
//        System.setProperty("javax.net.ssl.trustStore", truststore);
//        System.setProperty("javax.net.ssl.trustStorePassword", truststorePassword);

        MongoClient mongoClient = MongoClients.create(connectionString);

        MongoDatabase testDB = mongoClient.getDatabase("sample-database");
        numbersCollection = testDB.getCollection("sample-collection");

        CountDownLatch latch = new CountDownLatch(request.getInsertCount());
        long startTime = System.currentTimeMillis();

        for (int i = 0; i < request.getInsertCount(); i++) {
            executor.submit(() -> {
                try {
                    Document docCopy = Document.parse(baseDocuments.get(0).toJson());
                    docCopy.remove("_id");
                    docCopy.put("orderID", "ORD" + ThreadLocalRandom.current().nextInt(10000, 100000));

                    long insertStart = System.currentTimeMillis();
//                    collection.insertOne(docCopy);
                    numbersCollection.insertOne(docCopy);
                    long insertTime = System.currentTimeMillis() - insertStart;

                    long currentMillis = System.currentTimeMillis();
                    long currentElapsedSeconds = (currentMillis - startTime) / 1000;

                    log.info(" [Second {}] Thread {}: Insert completed in {} ms ",
                            currentElapsedSeconds,
                            Thread.currentThread().getId(),
                            insertTime);
                } finally {
                    latch.countDown();
                }
            });
        }

        awaitLatch(latch);
        executor.shutdown();
        long totalTime = System.currentTimeMillis() - startTime;
        log.info("All {} documents inserted in {} seconds", request.getInsertCount(), totalTime / 1000.0);
    }

    /**
     * Performs a concurrent read test on the database using multiple threads.
     * Each thread reads a document based on generated orderID.
     *
     * @param request LoadTestRequest containing read count and thread configuration
     */
    @Override
    public void readTest(LoadTestRequest request) {
        ExecutorService executor = Executors.newFixedThreadPool(request.getThreadCounts());
//        MongoDatabase db = mongoTemplate.getDb();
//        MongoCollection<Document> collection = db.getCollection("testCollection");

//        System.setProperty("javax.net.ssl.trustStore", truststore);
//        System.setProperty("javax.net.ssl.trustStorePassword", truststorePassword);

        MongoClient mongoClient = MongoClients.create(connectionString);

        MongoDatabase testDB = mongoClient.getDatabase("sample-database");
        numbersCollection = testDB.getCollection("sample-collection");
        CountDownLatch latch = new CountDownLatch(request.getReadCount());
        long startTime = System.currentTimeMillis();

        for (int i = 1; i <= request.getReadCount(); i++) {
            int finalDocIndex = i;
            executor.submit(() -> {
                try {
                    int temp = 2000 + ((finalDocIndex > 4000) ? finalDocIndex - 4000 : finalDocIndex);

                    long readStart = System.currentTimeMillis();
                    numbersCollection.find(new Document("orderID", "ORD" + temp)).first();
                    long readTime = System.currentTimeMillis() - readStart;
                    long currentMillis = System.currentTimeMillis();
                    long currentElapsedSeconds = (currentMillis - startTime) / 1000;

                    log.info(" [Second {}] Thread {}: Read completed in {} ms )",
                            currentElapsedSeconds,
                            Thread.currentThread().getId(),
                            readTime);
                } finally {
                    latch.countDown();
                }
            });
        }

        awaitLatch(latch);
        executor.shutdown();
        long totalTime = System.currentTimeMillis() - startTime;
        log.info("All {} reads completed in {} seconds", request.getReadCount(), totalTime / 1000.0);
    }

    /**
     * Performs a concurrent update test on the database using multiple threads.
     * Updates documents by adding paxGroup and paymentFunctions fields.
     *
     * @param request LoadTestRequest containing update count and thread configuration
     */
    @Override
    public void updateTest(LoadTestRequest request) {
        String paxGroupJson = "{ \"contactInfo\": [\"contact1@example.com\", \"contact2@example.com\"], \"paxGroupId\": \"PG123\", \"paxGroupName\": \"Team Alpha\", \"intendedPaxQty\": \"5\" }";
        String paymentFunJson = "{ \"paymentFunctions\": [{ \"orderAssociation\": {\"OrderItemRefID\": \"ORDITM46\", \"OrderRefID\": \"XB089279097\"}, \"paymentProcessingSummary\": {\"amount\": 5533, \"paymentCommitmentDateTime\": \"1986-11-10T19:09:51\", \"paymentID\": \"PAY05\", \"paymentProcessingSummaryPaymentMethod\": {\"paymentCard\": {\"cardBrandCode\": \"JCB 16 digit\", \"cardHolderName\": \"Michael Archer\", \"maskedCardID\": \"3584893897434748\"}}, \"paymentStatusCode\": \"FAILED\"}}]}";

        Document paxGroupDoc = Document.parse(paxGroupJson);
        Document paymentFunDoc = Document.parse(paymentFunJson);

        ExecutorService executor = Executors.newFixedThreadPool(request.getThreadCounts());
//        MongoDatabase db = mongoTemplate.getDb();
//        MongoCollection<Document> collection = db.getCollection("testCollection");

//        System.setProperty("javax.net.ssl.trustStore", truststore);
//        System.setProperty("javax.net.ssl.trustStorePassword", truststorePassword);

        MongoClient mongoClient = MongoClients.create(connectionString);

        MongoDatabase testDB = mongoClient.getDatabase("sample-database");
        numbersCollection = testDB.getCollection("sample-collection");
        CountDownLatch latch = new CountDownLatch(request.getUpdateCount());
        long startTime = System.currentTimeMillis();

        for (int i = 1; i <= request.getUpdateCount(); i++) {
            int finalDocIndex = i;
            executor.submit(() -> {
                try {
                    int temp = 2000 + ((finalDocIndex > 4000) ? finalDocIndex - 4000 : finalDocIndex);
                    Bson filter = Filters.eq("orderID", "ORD" + temp);
                    Bson update = Updates.combine(
                            Updates.set("paxGroup", paxGroupDoc),
                            Updates.set("paymentFunctions", paymentFunDoc),
                            Updates.set("orderItem.$[].statusCode", "CANCELLED")
                    );

                    long updateStart = System.currentTimeMillis();
                    numbersCollection.updateOne(filter, update);
                    long updateTime = System.currentTimeMillis() - updateStart;

                    long currentMillis = System.currentTimeMillis();
                    long currentElapsedSeconds = (currentMillis - startTime) / 1000;

                    log.info(" [Second {}] Thread {}: Update completed in {} ms)",
                            currentElapsedSeconds,
                            Thread.currentThread().getId(),
                            updateTime);
                } finally {
                    latch.countDown();
                }
            });
        }

        awaitLatch(latch);
        executor.shutdown();
        long totalTime = System.currentTimeMillis() - startTime;
        log.info("All {} updates completed in {} seconds", request.getUpdateCount(), totalTime / 1000.0);
    }

    /**
     * Inserts base documents into the database to prepare data for read and update tests.
     * Base documents are loaded first from specific JSON files if not already loaded.
     */
    @Override
    public void insertDataForReadAndUpdate() {
        loadBaseDocuments(List.of("orders_2001_3000.json","orders_3001_4000.json"));
//        MongoDatabase db = mongoTemplate.getDb();
//        MongoCollection<Document> collection = db.getCollection("testCollection");
//        System.setProperty("javax.net.ssl.trustStore", truststore);
//        System.setProperty("javax.net.ssl.trustStorePassword", truststorePassword);

        MongoClient mongoClient = MongoClients.create(connectionString);

        MongoDatabase testDB = mongoClient.getDatabase("sample-database");
        numbersCollection = testDB.getCollection("sample-collection");
        log.info("Storing Docs for Read and Update ........");
        for (int i = 0; i < baseDocuments.size(); i++) {
            try {
                Document docCopy = Document.parse(baseDocuments.get(0).toJson());
                docCopy.remove("_id");
                docCopy.put("orderID", "ORD" + ThreadLocalRandom.current().nextInt(10000, 100000));
                numbersCollection.insertOne(docCopy);

            } catch(Exception exception){
                log.error(exception.getMessage());
            }
        }
        baseDocuments.clear();
        log.info("Stored All the Docs for Read and Update");
    }

    /**
     * Waits for a given CountDownLatch to complete, handling interruptions properly.
     *
     * @param latch CountDownLatch to wait on
     */
    private void awaitLatch(CountDownLatch latch) {
        try {
            latch.await();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }

}
