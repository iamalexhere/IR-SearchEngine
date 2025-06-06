/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package com.IR.SearchEngine.data;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/**
 * Represents the result of a query in the search engine.
 * This class encapsulates the ranked list of documents that match a query.
 * 
 * Responsibilities:
 * - Store the ranked list of documents with their scores
 * - Provide methods to access and iterate through results
 * - Support pagination and result filtering
 * - Maintain query metadata (execution time, etc.)
 * 
 * Implementation notes:
 * - Supports efficient sorting and filtering
 * - Works with different retrieval models (VSM, BM25, etc.)
 * - May include snippets or highlights for display purposes
 * 
 * @author alexhere
 */
public class QueryResult {
    
    private final String originalQuery;
    private final String processedQuery;
    private final List<DocumentScore> results;
    private final long executionTimeMs;
    private final String modelName; // E.g., "VSM", "BM25", etc.
    
    /**
     * Creates a new query result with the specified parameters.
     * 
     * @param originalQuery The original query as entered by the user
     * @param processedQuery The processed query after preprocessing
     * @param results The list of document scores for this query
     * @param executionTimeMs The execution time in milliseconds
     * @param modelName The name of the retrieval model used (e.g., "VSM", "BM25")
     */
    public QueryResult(String originalQuery, String processedQuery, List<DocumentScore> results, 
                      long executionTimeMs, String modelName) {
        this.originalQuery = originalQuery;
        this.processedQuery = processedQuery;
        this.results = results != null ? results : new ArrayList<>();
        this.executionTimeMs = executionTimeMs;
        this.modelName = modelName;
    }
    
    /**
     * Creates a new query result with the specified parameters.
     * Uses "Unknown" as the default model name.
     * 
     * @param originalQuery The original query as entered by the user
     * @param processedQuery The processed query after preprocessing
     * @param results The list of document scores for this query
     * @param executionTimeMs The execution time in milliseconds
     */
    public QueryResult(String originalQuery, String processedQuery, List<DocumentScore> results, 
                      long executionTimeMs) {
        this(originalQuery, processedQuery, results, executionTimeMs, "Unknown");
    }
    
    /**
     * Gets the original query as entered by the user.
     * 
     * @return The original query
     */
    public String getOriginalQuery() {
        return originalQuery;
    }
    
    /**
     * Gets the processed query after preprocessing.
     * 
     * @return The processed query
     */
    public String getProcessedQuery() {
        return processedQuery;
    }
    
    /**
     * Gets the list of document scores for this query.
     * 
     * @return The list of document scores
     */
    public List<DocumentScore> getResults() {
        return results;
    }
    
    /**
     * Gets the execution time in milliseconds.
     * 
     * @return The execution time
     */
    public long getExecutionTimeMs() {
        return executionTimeMs;
    }
    
    /**
     * Gets the name of the retrieval model used.
     * 
     * @return The model name
     */
    public String getModelName() {
        return modelName;
    }
    
    /**
     * Gets the number of results.
     * 
     * @return The result count
     */
    public int getResultCount() {
        return results.size();
    }
    
    /**
     * Gets a paginated subset of the results.
     * 
     * @param offset The offset to start from (0-based)
     * @param limit The maximum number of results to return
     * @return A sublist of the results
     */
    public List<DocumentScore> getPaginatedResults(int offset, int limit) {
        if (offset < 0 || offset >= results.size()) {
            return new ArrayList<>();
        }
        
        int endIndex = Math.min(offset + limit, results.size());
        return results.subList(offset, endIndex);
    }
    
    /**
     * Gets the top K results.
     * 
     * @param k The number of top results to return
     * @return The top K results
     */
    public List<DocumentScore> getTopResults(int k) {
        return getPaginatedResults(0, k);
    }
    
    /**
     * Filters the results based on a minimum score threshold.
     * 
     * @param minScore The minimum score threshold
     * @return A new QueryResult with filtered results
     */
    public QueryResult filterByMinScore(double minScore) {
        List<DocumentScore> filteredResults = new ArrayList<>();
        
        for (DocumentScore score : results) {
            if (score.getScore() >= minScore) {
                filteredResults.add(score);
            }
        }
        
        return new QueryResult(originalQuery, processedQuery, filteredResults, executionTimeMs, modelName);
    }
    
    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append("Query: ").append(originalQuery).append("\n");
        sb.append("Processed: ").append(processedQuery).append("\n");
        sb.append("Model: ").append(modelName).append("\n");
        sb.append("Found ").append(results.size()).append(" results in ").append(executionTimeMs).append(" ms\n");
        
        int rank = 1;
        for (DocumentScore result : results) {
            sb.append(rank++).append(". ");
            sb.append(result.getDocument().getTitle());
            sb.append(" (score: ").append(String.format("%.4f", result.getScore())).append(")\n");
        }
        
        return sb.toString();
    }
}
