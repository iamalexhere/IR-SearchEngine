/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package com.IR.SearchEngine.model;

import com.IR.SearchEngine.data.Document;
import com.IR.SearchEngine.data.DocumentScore;
import com.IR.SearchEngine.data.QueryResult;
import com.IR.SearchEngine.indexing.Indexer;
import com.IR.SearchEngine.preprocessing.Preprocessor;

import java.util.*;
import java.util.stream.Collectors;

/**
 * Implements the BM25 ranking model for document retrieval.
 * BM25 is a probabilistic ranking function that extends the basic TF-IDF model.
 * 
 * BM25 Formula:
 * 
 * score(D,Q) = ∑(for term t in query Q) IDF(t) · f(t,D) · (k1 + 1) / (f(t,D) + k1 · (1 - b + b · |D|/avgdl))
 * 
 * Where:
 * - f(t,D) is the frequency of term t in document D
 * - |D| is the length of document D in words
 * - avgdl is the average document length in the corpus
 * - k1 is a parameter that controls term frequency scaling (typically 1.2-2.0)
 * - b is a parameter that controls document length normalization (typically 0.75)
 * - IDF(t) = log((N - n(t) + 0.5) / (n(t) + 0.5))
 *   where N is the total number of documents and n(t) is the number of documents containing term t
 * 
 * Responsibilities:
 * - Calculate BM25 scores for query-document pairs
 * - Handle parameter tuning (k1, b) for optimal performance
 * - Provide document length normalization
 * - Support incremental scoring for efficient query processing
 * 
 * Implementation notes:
 * - Pre-computes document length statistics during indexing
 * - Implements optimizations for efficient scoring
 * 
 * @author alexhere
 */
public class BM25 implements IModel {
    
    private final Indexer indexer;
    private final Preprocessor preprocessor;
    
    // BM25 parameters
    private final double k1; // Controls term frequency scaling (typically 1.2-2.0)
    private final double b;  // Controls document length normalization (typically 0.75)
    
    // Precomputed statistics
    private double avgDocLength;
    private final Map<Integer, Double> documentLengths;
    private final Map<String, Double> idfCache;
    
    /**
     * Constructor with default BM25 parameters (k1=1.2, b=0.75).
     * 
     * @param indexer The indexer containing document information and term statistics
     * @param preprocessor The preprocessor for query processing
     */
    public BM25(Indexer indexer, Preprocessor preprocessor) {
        this(indexer, preprocessor, 1.2, 0.75);
    }
    
    /**
     * Constructor with customizable BM25 parameters.
     * 
     * @param indexer The indexer containing document information and term statistics
     * @param preprocessor The preprocessor for query processing
     * @param k1 Parameter that controls term frequency scaling
     * @param b Parameter that controls document length normalization
     */
    public BM25(Indexer indexer, Preprocessor preprocessor, double k1, double b) {
        if (indexer == null) {
            throw new IllegalArgumentException("Indexer cannot be null");
        }
        if (preprocessor == null) {
            throw new IllegalArgumentException("Preprocessor cannot be null");
        }
        
        this.indexer = indexer;
        this.preprocessor = preprocessor;
        this.k1 = k1;
        this.b = b;
        this.documentLengths = new HashMap<>();
        this.idfCache = new HashMap<>();
        this.avgDocLength = 0.0;
    }
    
    /**
     * Gets the name of this retrieval model.
     * 
     * @return The model name ("BM25")
     */
    @Override
    public String getModelName() {
        return "BM25";
    }
    
    /**
     * Initializes the model by precomputing document length statistics.
     * Called after documents have been indexed.
     */
    @Override
    public void initialize() {
        System.out.println("Initializing BM25 model...");
        precomputeDocumentStatistics();
    }
    
    /**
     * Precomputes document length statistics for BM25 scoring.
     */
    private void precomputeDocumentStatistics() {
        List<Document> allDocs = indexer.getAllDocuments();
        System.out.println("Computing document statistics for " + allDocs.size() + " documents");
        
        // Compute document lengths and average document length
        long totalLength = 0;
        for (int i = 0; i < allDocs.size(); i++) {
            Document doc = allDocs.get(i);
            int docLength = doc.getLength();
            documentLengths.put(i, (double) docLength);
            totalLength += docLength;
            
            // Print debug info for the first few documents
            if (i < 3) {
                System.out.println("Document: " + doc.getTitle() + 
                                 " (ID: " + doc.getId() + ", length: " + docLength + ")");
            }
        }
        
        avgDocLength = allDocs.isEmpty() ? 0 : (double) totalLength / allDocs.size();
        System.out.println("Average document length: " + avgDocLength);
        System.out.println("BM25 parameters: k1=" + k1 + ", b=" + b);
    }
    
    /**
     * Computes the BM25 weighted vector for a document.
     * This is used primarily for debugging/comparison with VSM vectors.
     * 
     * @param document The document to compute the vector for
     * @return A map from terms to their BM25 weights
     */
    @Override
    public Map<String, Double> computeDocumentVector(Document document) {
        Map<String, Double> vector = new HashMap<>();
        Map<String, Integer> termFrequencies = document.getTermFrequencies();
        double docLength = document.getLength();
        
        // For each term in the document, compute its BM25 weight component
        for (Map.Entry<String, Integer> entry : termFrequencies.entrySet()) {
            String term = entry.getKey();
            int tf = entry.getValue();
            
            // Skip terms not in the index vocabulary
            if (!indexer.getVocabulary().contains(term)) {
                continue;
            }
            
            double idf = computeIdf(term);
            double weight = computeBM25TermWeight(tf, docLength, idf);
            vector.put(term, weight);
        }
        
        return vector;
    }
    
    /**
     * Computes the BM25 weighted vector for a query.
     * 
     * @param queryTerms The preprocessed query terms with their frequencies
     * @return A map from terms to their BM25 weights
     */
    @Override
    public Map<String, Double> computeQueryVector(Map<String, Integer> queryTerms) {
        Map<String, Double> queryVector = new HashMap<>();
        
        // For each term in the query, compute its BM25 weight
        for (Map.Entry<String, Integer> entry : queryTerms.entrySet()) {
            String term = entry.getKey();
            
            // Skip terms not in the vocabulary
            if (!indexer.getVocabulary().contains(term)) {
                continue;
            }
            
            // In BM25, query terms are usually weighted just by their IDF
            double idf = computeIdf(term);
            queryVector.put(term, idf);
        }
        
        return queryVector;
    }
    
    /**
     * Computes the BM25 IDF value for a term.
     * 
     * @param term The term to compute IDF for
     * @return The BM25 IDF value
     */
    private double computeIdf(String term) {
        // Check if IDF is already cached
        if (idfCache.containsKey(term)) {
            return idfCache.get(term);
        }
        
        int N = indexer.getDocumentCount();
        int n = indexer.getDocumentFrequency(term);
        
        // BM25 IDF formula: log((N - n + 0.5) / (n + 0.5))
        double idf = Math.log((N - n + 0.5) / (n + 0.5));
        
        // Ensure IDF is positive (some formulations use max(0, idf))
        idf = Math.max(0, idf);
        
        // Cache the result
        idfCache.put(term, idf);
        
        return idf;
    }
    
    /**
     * Computes the BM25 term weight component for a term in a document.
     * 
     * @param tf Term frequency in the document
     * @param docLength Length of the document
     * @param idf IDF value for the term
     * @return The BM25 term weight
     */
    private double computeBM25TermWeight(int tf, double docLength, double idf) {
        // BM25 term weight formula: idf * ((tf * (k1 + 1)) / (tf + k1 * (1 - b + b * docLength / avgDocLength)))
        double numerator = tf * (k1 + 1);
        double denominator = tf + k1 * (1 - b + b * docLength / avgDocLength);
        
        return idf * (numerator / denominator);
    }
    
    /**
     * Executes a search for the given query and returns top K results.
     * 
     * @param query Original query string
     * @param processedQuery Preprocessed query string
     * @param topK Number of top results to return
     * @return The search results with document IDs and similarity scores
     */
    @Override
    public QueryResult search(String query, String processedQuery, int topK) {
        long startTime = System.currentTimeMillis();
        
        // Debug log query information
        System.out.println("Executing BM25 search for query: " + query);
        System.out.println("Processed query: " + processedQuery);
        
        // Preprocess query and convert to term frequency map
        Map<String, Integer> queryTerms = processQueryToTermFrequencies(processedQuery);
        System.out.println("Query terms: " + queryTerms.keySet());
        
        // Get all documents for scoring
        List<Document> allDocs = indexer.getAllDocuments();
        System.out.println("Scoring " + allDocs.size() + " documents with BM25");
        
        // Initialize results list
        List<DocumentScore> results = new ArrayList<>();
        
        // Score each document
        for (int docId = 0; docId < allDocs.size(); docId++) {
            Document doc = allDocs.get(docId);
            double score = computeBM25Score(queryTerms, doc, docId);
            
            // Add to results if score is positive and above threshold
            if (score > 0.01) {
                results.add(new DocumentScore(doc, score, "BM25"));
                
                // Debug log for matching documents
                System.out.println("Match found: " + doc.getTitle() + " (score: " + score + ")");
            }
        }
        
        // Sort by score (descending)
        results.sort(Comparator.comparing(DocumentScore::getScore).reversed());
        System.out.println("Found " + results.size() + " matching documents");
        
        // Limit to top K results
        List<DocumentScore> topResults = results.stream()
                .limit(topK)
                .collect(Collectors.toList());
        
        long endTime = System.currentTimeMillis();
        long executionTime = endTime - startTime;
        
        // Create a QueryResult with the BM25 model name
        return new QueryResult(query, processedQuery, topResults, executionTime, "BM25");
    }
    
    /**
     * Computes the BM25 score for a document with respect to a query.
     * 
     * @param queryTerms The query terms with their frequencies
     * @param doc The document to score
     * @param docId The document ID for statistics lookup
     * @return The BM25 score
     */
    private double computeBM25Score(Map<String, Integer> queryTerms, Document doc, int docId) {
        double score = 0.0;
        double docLength = documentLengths.getOrDefault(docId, (double) doc.getLength());
        Map<String, Integer> docTermFreqs = doc.getTermFrequencies();
        
        // For each term in the query
        for (String term : queryTerms.keySet()) {
            // Skip terms not in the document
            if (!docTermFreqs.containsKey(term)) {
                continue;
            }
            
            int tf = docTermFreqs.get(term);
            double idf = computeIdf(term);
            
            // Compute BM25 term weight and add to score
            score += computeBM25TermWeight(tf, docLength, idf);
        }
        
        return score;
    }
    
    /**
     * Converts a preprocessed query string to a map of term frequencies.
     * 
     * @param processedQuery The preprocessed query string
     * @return Map of terms to their frequencies in the query
     */
    private Map<String, Integer> processQueryToTermFrequencies(String processedQuery) {
        String[] terms = processedQuery.split("\\s+");
        Map<String, Integer> termFreqs = new HashMap<>();
        
        for (String term : terms) {
            if (!term.isEmpty()) {
                termFreqs.put(term, termFreqs.getOrDefault(term, 0) + 1);
            }
        }
        
        return termFreqs;
    }
}
