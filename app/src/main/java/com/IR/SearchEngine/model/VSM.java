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
 * Implements the Vector Space Model for document retrieval.
 * VSM represents documents and queries as vectors in a high-dimensional space.
 * 
 * Responsibilities:
 * - Calculate TF-IDF weights for terms in documents and queries
 * - Compute cosine similarity between document and query vectors
 * - Support different term weighting schemes (binary, TF, TF-IDF)
 * - Handle document length normalization
 * 
 * Implementation notes:
 * - Uses sparse vector representation for memory efficiency
 * - Implements optimized cosine similarity calculation
 * - Caches document vectors for improved performance
 * 
 * @author alexhere
 */
/**
 * Vector Space Model (VSM) Implementation
 * 
 * The Vector Space Model represents documents and queries as vectors in a high-dimensional space where
 * each dimension corresponds to a term in the vocabulary. Similarity between documents and queries
 * is measured using the cosine of the angle between their vectors.
 * 
 * Key formulas:
 * 
 * 1. TF-IDF weight for term t in document d:
 *    w(t,d) = tf(t,d) * idf(t)
 * 
 * 2. Term Frequency variants:
 *    - Binary: tf(t,d) = 1 if term exists, 0 otherwise
 *    - Raw frequency: tf(t,d) = frequency of term t in document d
 *    - Log normalization: tf(t,d) = 1 + log(frequency of term t in document d)
 *    - Augmented frequency: tf(t,d) = 0.5 + 0.5 * (frequency of term t in document d / max frequency in d)
 * 
 * 3. Inverse Document Frequency:
 *    idf(t) = log(N / df(t))
 *    where N is the total number of documents, df(t) is the number of documents containing term t
 * 
 * 4. Cosine Similarity between document d and query q:
 *    sim(d,q) = (d · q) / (|d| * |q|)
 *    = sum(w(t,d) * w(t,q) for all t) / sqrt(sum(w(t,d)^2 for all t) * sum(w(t,q)^2 for all t))
 */
public class VSM implements IModel {
    
    private final Indexer indexer;
    private final Preprocessor preprocessor;
    private final Map<Integer, Map<String, Double>> documentVectors;
    private final Map<Integer, Double> documentVectorNorms;
    
    // Weight constants for term frequency variants
    private static final int TF_BINARY = 0;
    private static final int TF_RAW = 1;
    private static final int TF_LOG = 2;
    private static final int TF_AUGMENTED = 3;
    
    // Default weighting scheme
    private final int tfWeightingScheme;
    
    /**
     * Constructor initializing the VSM with an indexer and preprocessor.
     * Uses default TF-LOG weighting scheme.
     * 
     * @param indexer The indexer containing document information and term statistics
     * @param preprocessor The preprocessor for query processing
     */
    public VSM(Indexer indexer, Preprocessor preprocessor) {
        this(indexer, preprocessor, TF_LOG);
    }
    
    /**
     * Constructor initializing the VSM with specified components and weighting scheme.
     * 
     * @param indexer The indexer containing document information and term statistics
     * @param preprocessor The preprocessor for query processing
     * @param tfWeightingScheme The term frequency weighting scheme to use
     */
    public VSM(Indexer indexer, Preprocessor preprocessor, int tfWeightingScheme) {
        if (indexer == null) {
            throw new IllegalArgumentException("Indexer cannot be null");
        }
        if (preprocessor == null) {
            throw new IllegalArgumentException("Preprocessor cannot be null");
        }
        
        this.indexer = indexer;
        this.preprocessor = preprocessor;
        this.documentVectors = new HashMap<>();
        this.documentVectorNorms = new HashMap<>();
        this.tfWeightingScheme = tfWeightingScheme;
        
        // Precompute document vectors for all documents in the index
        precomputeAllDocumentVectors();
    }
    
    /**
     * Gets the name of this retrieval model.
     * 
     * @return The model name ("VSM")
     */
    @Override
    public String getModelName() {
        return "VSM";
    }
    
    /**
     * Initializes the model by precomputing document vectors.
     * Called after documents have been indexed.
     */
    @Override
    public void initialize() {
        precomputeAllDocumentVectors();
    }
    
    /**
     * Precomputes TF-IDF vectors for all documents to improve search performance.
     * This is called during initialization.
     */
    private void precomputeAllDocumentVectors() {
        System.out.println("Precomputing document vectors for " + indexer.getDocumentCount() + " documents");
        List<Document> allDocs = indexer.getAllDocuments();
        
        // Create a mapping from document ID string to internal integer ID
        Map<String, Integer> docIdMap = new HashMap<>();
        for (int i = 0; i < allDocs.size(); i++) {
            docIdMap.put(allDocs.get(i).getId(), i);
        }
        
        for (int i = 0; i < allDocs.size(); i++) {
            Document doc = allDocs.get(i);
            Map<String, Double> docVector = computeDocumentVector(doc);
            documentVectors.put(i, docVector);
            documentVectorNorms.put(i, computeVectorNorm(docVector));
            
            // Print debug info for the first few documents
            if (i < 3) {
                System.out.println("Computed vector for document: " + doc.getTitle() + 
                                 " (ID: " + doc.getId() + ", internal ID: " + i + ")");
                System.out.println("Vector size: " + docVector.size() + " terms");
            }
        }
        System.out.println("Finished precomputing document vectors");
    }
    
    /**
     * Computes the TF-IDF weighted vector for a document.
     * 
     * @param document The document to compute the vector for
     * @return A map from terms to their TF-IDF weights
     */
    public Map<String, Double> computeDocumentVector(Document document) {
        Map<String, Double> vector = new HashMap<>();
        Map<String, Integer> termFrequencies = document.getTermFrequencies();
        
        // For each term in the document, compute its TF-IDF weight
        for (Map.Entry<String, Integer> entry : termFrequencies.entrySet()) {
            String term = entry.getKey();
            int rawTF = entry.getValue();
            
            // Skip terms not in the index vocabulary (should not happen in normal operation)
            if (!indexer.getVocabulary().contains(term)) {
                continue;
            }
            
            // Compute the weighted TF component based on the selected scheme
            double weightedTF = computeWeightedTF(rawTF, document.getLength());
            
            // Get the IDF value from the indexer
            double idf = indexer.getIdf(term);
            
            // Compute the final TF-IDF weight
            double tfIdf = weightedTF * idf;
            vector.put(term, tfIdf);
        }
        
        return vector;
    }
    
    /**
     * Computes the TF-IDF weighted vector for a query.
     * 
     * @param processedQuery The preprocessed query terms with their frequencies
     * @return A map from terms to their TF-IDF weights
     */
    public Map<String, Double> computeQueryVector(Map<String, Integer> processedQuery) {
        Map<String, Double> queryVector = new HashMap<>();
        int queryLength = processedQuery.values().stream().mapToInt(Integer::intValue).sum();
        
        // For each term in the query, compute its TF-IDF weight
        for (Map.Entry<String, Integer> entry : processedQuery.entrySet()) {
            String term = entry.getKey();
            int rawTF = entry.getValue();
            
            // Skip terms not in the vocabulary
            if (!indexer.getVocabulary().contains(term)) {
                continue;
            }
            
            // Compute the weighted TF component
            double weightedTF = computeWeightedTF(rawTF, queryLength);
            
            // Get the IDF value
            double idf = indexer.getIdf(term);
            
            // Compute the TF-IDF weight
            double tfIdf = weightedTF * idf;
            queryVector.put(term, tfIdf);
        }
        
        return queryVector;
    }
    
    /**
     * Computes the weighted term frequency based on the selected weighting scheme.
     * 
     * @param rawTF The raw term frequency
     * @param docLength The document length (total terms)
     * @return The weighted term frequency
     */
    private double computeWeightedTF(int rawTF, int docLength) {
        switch (tfWeightingScheme) {
            case TF_BINARY:
                // Binary weighting: 1 if term exists, 0 otherwise
                return rawTF > 0 ? 1.0 : 0.0;
                
            case TF_RAW:
                // Raw frequency: use the raw count
                return rawTF;
                
            case TF_LOG:
                // Logarithmic weighting: 1 + log(tf)
                return rawTF > 0 ? 1.0 + Math.log10(rawTF) : 0.0;
                
            case TF_AUGMENTED:
                // Augmented frequency: prevents bias towards longer documents
                // 0.5 + 0.5 * (tf / max_tf)
                return 0.5 + 0.5 * ((double) rawTF / docLength);
                
            default:
                // Default to log weighting
                return rawTF > 0 ? 1.0 + Math.log10(rawTF) : 0.0;
        }
    }
    
    /**
     * Computes the Euclidean norm (magnitude) of a vector.
     * 
     * @param vector The vector as a map from terms to weights
     * @return The Euclidean norm of the vector
     */
    private double computeVectorNorm(Map<String, Double> vector) {
        double sumOfSquares = vector.values().stream()
                .mapToDouble(weight -> weight * weight)
                .sum();
        return Math.sqrt(sumOfSquares);
    }
    
    /**
     * Computes the cosine similarity between two vectors.
     * 
     * @param vector1 The first vector
     * @param vector2 The second vector
     * @return The cosine similarity value [0,1]
     */
    public double computeCosineSimilarity(Map<String, Double> vector1, Map<String, Double> vector2) {
        // Ensure we use the smaller vector as the first one for efficiency
        if (vector1.size() > vector2.size()) {
            Map<String, Double> temp = vector1;
            vector1 = vector2;
            vector2 = temp;
        }
        
        double dotProduct = 0.0;
        double norm1 = computeVectorNorm(vector1);
        double norm2 = computeVectorNorm(vector2);
        
        // If either vector has zero magnitude, similarity is 0
        if (norm1 == 0 || norm2 == 0) {
            return 0.0;
        }
        
        // Compute dot product by iterating through the smaller vector
        for (Map.Entry<String, Double> entry : vector1.entrySet()) {
            String term = entry.getKey();
            Double weight1 = entry.getValue();
            Double weight2 = vector2.get(term);
            
            if (weight2 != null) {
                dotProduct += weight1 * weight2;
            }
        }
        
        // Cosine similarity formula: dot_product / (|v1| * |v2|)
        return dotProduct / (norm1 * norm2);
    }
    
    /**
     * Executes a search for the given query and returns top K results.
     * 
     * @param query Original query string
     * @param processedQuery Preprocessed query string
     * @param topK Number of top results to return
     * @return The search results with document IDs and similarity scores
     */
    public QueryResult search(String query, String processedQuery, int topK) {
        long startTime = System.currentTimeMillis();
        
        // Debug log query information
        System.out.println("Executing search for query: " + query);
        System.out.println("Processed query: " + processedQuery);
        
        // Preprocess query and convert to term frequency map
        Map<String, Integer> queryTermFreqs = processQueryToTermFrequencies(processedQuery);
        System.out.println("Query terms: " + queryTermFreqs.keySet());
        
        // Compute query vector
        Map<String, Double> queryVector = computeQueryVector(queryTermFreqs);
        System.out.println("Query vector size: " + queryVector.size() + " terms");
        
        // Initialize results list
        List<DocumentScore> results = new ArrayList<>();
        
        // Get all documents for matching
        List<Document> allDocs = indexer.getAllDocuments();
        System.out.println("Comparing query to " + allDocs.size() + " documents");
        
        // For each document, compute similarity with the query
        for (Map.Entry<Integer, Map<String, Double>> entry : documentVectors.entrySet()) {
            int docId = entry.getKey();
            Map<String, Double> docVector = entry.getValue();
            
            // Make sure docId is valid
            if (docId >= 0 && docId < allDocs.size()) {
                // Compute similarity
                double similarity = computeCosineSimilarity(queryVector, docVector);
                
                // Add to results if similarity is positive and above threshold
                if (similarity > 0.01) { // Use a small threshold to filter out very low similarities
                    Document doc = allDocs.get(docId);
                    // Create a DocumentScore with TF-IDF score type
                    results.add(new DocumentScore(doc, similarity, "TF-IDF"));
                    
                    // Debug log for matching documents
                    System.out.println("Match found: " + doc.getTitle() + " (score: " + similarity + ")");
                }
            }
        }
        
        // Sort by similarity score (descending)
        results.sort(Comparator.comparing(DocumentScore::getScore).reversed());
        System.out.println("Found " + results.size() + " matching documents");
        
        // Limit to top K results
        List<DocumentScore> topResults = results.stream()
                .limit(topK)
                .collect(Collectors.toList());
        
        long endTime = System.currentTimeMillis();
        long executionTime = endTime - startTime;
        
        // Create a QueryResult with the VSM model name
        return new QueryResult(query, processedQuery, topResults, executionTime, "VSM");
    }
    
    /**
     * Converts a preprocessed query string to a map of term frequencies.
     * 
     * @param processedQuery The preprocessed query string
     * @return Map of terms to their frequencies in the query
     */
    private Map<String, Integer> processQueryToTermFrequencies(String processedQuery) {
        // If the query is already preprocessed by an external component
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
