package com.example.demo.model;

import java.util.List;

/**
 * Model class representing the configuration for a database load test.
 * <p>
 * This class encapsulates the parameters used to control various aspects of the load test,
 * including the number of threads, file names for loading base documents, and the counts
 * for read, insert, and update operations.
 */
public class LoadTestRequest {

    /**
     * Number of concurrent threads to use during the test.
     */
    private int threadCounts;

    /**
     * List of JSON file names to load base documents from.
     */
    private List<String> fileNames;

    /**
     * Number of read operations to perform.
     */
    private int readCount;

    /**
     * Number of insert (write) operations to perform.
     */
    private int insertCount;

    /**
     * Number of update operations to perform.
     */
    private int updateCount;

    /**
     * Gets the number of concurrent threads to use during the test.
     *
     * @return number of threads
     */
    public int getThreadCounts() {
        return threadCounts;
    }

    /**
     * Sets the number of concurrent threads to use during the test.
     *
     * @param threadCounts number of threads
     */
    public void setThreadCounts(int threadCounts) {
        this.threadCounts = threadCounts;
    }

    /**
     * Gets the list of JSON file names used to load base documents.
     *
     * @return list of file names
     */
    public List<String> getFileNames() {
        return fileNames;
    }

    /**
     * Sets the list of JSON file names used to load base documents.
     *
     * @param fileNames list of file names
     */
    public void setFileNames(List<String> fileNames) {
        this.fileNames = fileNames;
    }

    /**
     * Gets the number of read operations to perform.
     *
     * @return number of reads
     */
    public int getReadCount() {
        return readCount;
    }

    /**
     * Sets the number of read operations to perform.
     *
     * @param readCount number of reads
     */
    public void setReadCount(int readCount) {
        this.readCount = readCount;
    }

    /**
     * Gets the number of insert (write) operations to perform.
     *
     * @return number of inserts
     */
    public int getInsertCount() {
        return insertCount;
    }

    /**
     * Sets the number of insert (write) operations to perform.
     *
     * @param insertCount number of inserts
     */
    public void setInsertCount(int insertCount) {
        this.insertCount = insertCount;
    }

    /**
     * Gets the number of update operations to perform.
     *
     * @return number of updates
     */
    public int getUpdateCount() {
        return updateCount;
    }

    /**
     * Sets the number of update operations to perform.
     *
     * @param updateCount number of updates
     */
    public void setUpdateCount(int updateCount) {
        this.updateCount = updateCount;
    }
}
