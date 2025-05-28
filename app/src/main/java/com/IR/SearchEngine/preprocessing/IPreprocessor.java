/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package com.IR.SearchEngine.preprocessing;

import com.IR.SearchEngine.data.Document;
import java.util.List;
import java.util.Map;

/**
 * Interface defining the contract for text preprocessing components.
 * All text preprocessing implementations should implement this interface.
 * 
 * Responsibilities:
 * - Define standard methods for text preprocessing operations
 * - Ensure compatibility between different preprocessing implementations
 * - Support configuration of preprocessing parameters
 * - Provide methods for preprocessing both documents and queries
 * 
 * Implementation notes:
 * - Will be implemented by concrete preprocessors
 * - Will leverage OpenNLP for advanced NLP functionality
 * 
 * @author alexhere
 */
public interface IPreprocessor {
    
    /**
     * Preprocesses a document, updating its processed content and term frequencies.
     * 
     * @param document The document to preprocess
     * @return The preprocessed document
     */
    Document preprocessDocument(Document document);
    
    /**
     * Preprocesses a batch of documents.
     * 
     * @param documents List of documents to preprocess
     * @return List of preprocessed documents
     */
    List<Document> preprocessDocuments(List<Document> documents);
    
    /**
     * Preprocesses a query string.
     * 
     * @param query The query string to preprocess
     * @return Preprocessed query string
     */
    String preprocessQuery(String query);
    
    /**
     * Tokenizes text into individual terms.
     * 
     * @param text Text to tokenize
     * @return Array of tokens
     */
    String[] tokenize(String text);
    
    /**
     * Removes stopwords from an array of tokens.
     * 
     * @param tokens Array of tokens
     * @return Array with stopwords removed
     */
    String[] removeStopwords(String[] tokens);
    
    /**
     * Applies stemming to an array of tokens.
     * 
     * @param tokens Array of tokens
     * @return Array of stemmed tokens
     */
    String[] stem(String[] tokens);
    
    /**
     * Computes term frequencies for an array of tokens.
     * 
     * @param tokens Array of tokens
     * @return Map of terms to their frequencies
     */
    Map<String, Integer> computeTermFrequencies(String[] tokens);
    
    /**
     * Normalizes text (lowercase, remove punctuation, etc.).
     * 
     * @param text Text to normalize
     * @return Normalized text
     */
    String normalizeText(String text);
    
    /**
     * Adds a custom stopword to the stopword list.
     * 
     * @param stopword Stopword to add
     */
    void addStopword(String stopword);
    
    /**
     * Sets whether stemming should be applied during preprocessing.
     * 
     * @param applyStemming True to apply stemming, false otherwise
     */
    void setApplyStemming(boolean applyStemming);
    
    /**
     * Sets whether stopword removal should be applied during preprocessing.
     * 
     * @param removeStopwords True to remove stopwords, false otherwise
     */
    void setRemoveStopwords(boolean removeStopwords);
}
