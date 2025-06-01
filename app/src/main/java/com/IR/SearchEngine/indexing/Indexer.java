/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package com.IR.SearchEngine.indexing;

import com.IR.SearchEngine.data.Document;
import com.IR.SearchEngine.data.Posting;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.Collections;

/**
 * Responsible for building and maintaining the inverted index.
 * This class processes documents and creates the search index.
 * 
 * Responsibilities:
 * - Process preprocessed documents and extract indexing terms
 * - Build and update the inverted index structure
 * - Compute document and collection statistics (IDF, etc.)
 * - Support incremental indexing and index maintenance
 * 
 * Implementation notes:
 * - Optimized for efficient batch processing
 * - Uses a custom inverted index implementation (no Lucene)
 * - Can be extended to support compression techniques for index storage
 * 
 * @author alexhere
 */
public class Indexer {
    
    private final InvertedIndex invertedIndex;
    private final Map<String, Double> idfValues;
    private final Map<Integer, Document> documents;
    private int nextDocId;
    private int documentCount;
    
    /**
     * Constructor that initializes the indexer with an empty inverted index.
     */
    public Indexer() {
        this.invertedIndex = new InvertedIndex();
        this.idfValues = new HashMap<>();
        this.documents = new HashMap<>();
        this.nextDocId = 0;
        this.documentCount = 0;
    }
    
    /**
     * Indexes a single document, adding its terms to the inverted index.
     * 
     * @param document The document to index
     * @return The document ID assigned to the document
     */
    public int indexDocument(Document document) {
        if (document == null) {
            throw new IllegalArgumentException("Document cannot be null");
        }
        
        int docId = nextDocId++;
        documents.put(docId, document);
        documentCount++;
        
        // Get the term frequencies from the document
        Map<String, Integer> termFrequencies = document.getTermFrequencies();
        if (termFrequencies == null || termFrequencies.isEmpty()) {
            System.err.println("Warning: Document " + document.getId() + " has no terms to index");
            return docId;
        }
        
        // Add each term to the inverted index
        int position = 0;
        for (Map.Entry<String, Integer> entry : termFrequencies.entrySet()) {
            String term = entry.getKey();
            int frequency = entry.getValue();
            
            // Add each occurrence of the term
            for (int i = 0; i < frequency; i++) {
                invertedIndex.addTerm(term, docId, position++);
            }
        }
        
        // IDF values will need to be recalculated
        idfValues.clear();
        
        return docId;
    }
    
    /**
     * Indexes a list of documents, building the inverted index.
     * 
     * @param documents The list of documents to index
     * @return The number of documents indexed
     */
    public int indexDocuments(List<Document> documents) {
        if (documents == null || documents.isEmpty()) {
            return 0;
        }
        
        int indexedCount = 0;
        for (Document document : documents) {
            indexDocument(document);
            indexedCount++;
        }
        
        // Calculate IDF values after all documents are indexed
        calculateAllIdfValues();
        
        return indexedCount;
    }
    
    /**
     * Calculates IDF values for all terms in the index.
     */
    private void calculateAllIdfValues() {
        Set<String> terms = invertedIndex.getVocabulary();
        
        for (String term : terms) {
            double idf = calculateIdf(term);
            idfValues.put(term, idf);
        }
    }
    
    /**
     * Calculates the IDF value for a specific term.
     * 
     * @param term The term to calculate IDF for
     * @return The IDF value
     */
    public double calculateIdf(String term) {
        int df = getDocumentFrequency(term);
        if (df == 0) {
            return 0.0;
        }
        
        // IDF = log(N/df) where N is the total number of documents
        return Math.log((double) documentCount / df);
    }
    
    /**
     * Gets the IDF value for a specific term.
     * Calculates it if not already computed.
     * 
     * @param term The term to get IDF for
     * @return The IDF value
     */
    public double getIdf(String term) {
        return idfValues.computeIfAbsent(term, this::calculateIdf);
    }
    
    /**
     * Gets the document frequency (number of documents containing the term) for a specific term.
     * 
     * @param term The term to get document frequency for
     * @return The document frequency
     */
    public int getDocumentFrequency(String term) {
        List<Posting> postings = invertedIndex.getPostings(term);
        return postings.size();
    }
    
    /**
     * Gets the postings list for a specific term.
     * 
     * @param term The term to get postings for
     * @return The postings list
     */
    public List<Posting> getPostings(String term) {
        return invertedIndex.getPostings(term);
    }
    
    /**
     * Gets the vocabulary (all indexed terms) from the inverted index.
     * 
     * @return The set of all terms in the index
     */
    public Set<String> getVocabulary() {
        return invertedIndex.getVocabulary();
    }
    
    /**
     * Gets the total number of documents in the index.
     * 
     * @return The document count
     */
    public int getDocumentCount() {
        return documentCount;
    }
    
    /**
     * Gets the vocabulary size (number of unique terms).
     * 
     * @return The vocabulary size
     */
    public int getVocabularySize() {
        return invertedIndex.getVocabulary().size();
    }
    
    /**
     * Gets the document with the specified ID.
     * 
     * @param docId The document ID
     * @return The document or null if not found
     */
    public Document getDocument(int docId) {
        return documents.get(docId);
    }
    
    /**
     * Gets all documents in the index.
     * 
     * @return An unmodifiable collection of all documents
     */
    public List<Document> getAllDocuments() {
        return new ArrayList<>(documents.values());
    }
}
