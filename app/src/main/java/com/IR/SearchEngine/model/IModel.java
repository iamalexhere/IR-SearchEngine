/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Interface.java to edit this template
 */
package com.IR.SearchEngine.model;

import com.IR.SearchEngine.data.Document;
import com.IR.SearchEngine.data.QueryResult;
import java.util.Map;

/**
 * Interface defining the contract for retrieval models.
 * All ranking models in the system should implement this interface.
 * 
 * Responsibilities:
 * - Define the standard methods for document scoring
 * - Ensure compatibility between different retrieval models
 * - Support model parameter configuration
 * - Provide methods for model evaluation and comparison
 * 
 * Implementation notes:
 * - Implemented by concrete models like BM25 and VSM
 * - Allows for easy switching between different retrieval models
 * 
 * @author alexhere
 */
public interface IModel {
    
    /**
     * Gets the name of the retrieval model.
     * 
     * @return The model name (e.g., "VSM", "BM25")
     */
    String getModelName();
    
    /**
     * Initializes the model with necessary data.
     * Called after documents have been indexed.
     */
    void initialize();
    
    /**
     * Executes a search for the given query and returns top K results.
     * 
     * @param query Original query string
     * @param processedQuery Preprocessed query string
     * @param topK Number of top results to return
     * @return The search results with document IDs and similarity scores
     */
    QueryResult search(String query, String processedQuery, int topK);
    
    /**
     * Computes a document vector based on the model's scoring mechanism.
     * 
     * @param document The document to compute the vector for
     * @return A map from terms to their weights according to the model
     */
    Map<String, Double> computeDocumentVector(Document document);
    
    /**
     * Computes a query vector based on the model's scoring mechanism.
     * 
     * @param queryTerms The preprocessed query terms with their frequencies
     * @return A map from terms to their weights according to the model
     */
    Map<String, Double> computeQueryVector(Map<String, Integer> queryTerms);
}
