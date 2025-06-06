/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package com.IR.SearchEngine.data;

/**
 * Represents a document with its relevance score in search results.
 * This class associates a document with its score from a retrieval model.
 * 
 * Responsibilities:
 * - Link a document to its relevance score
 * - Support comparison for ranking purposes
 * - Provide access to document metadata and score
 * 
 * Implementation notes:
 * - Works with different retrieval models (VSM, BM25, etc.)
 * - Used by QueryResult to represent ranked search results
 * 
 * @author alexhere
 */
public class DocumentScore implements Comparable<DocumentScore> {
    
    private final Document document;
    private final double score;
    private final String scoreType; // Optional, e.g., "TF-IDF", "BM25", etc.
    
    /**
     * Creates a new document score with the specified document and score.
     * 
     * @param document The document
     * @param score The relevance score
     */
    public DocumentScore(Document document, double score) {
        this(document, score, "Unknown");
    }
    
    /**
     * Creates a new document score with the specified document, score, and score type.
     * 
     * @param document The document
     * @param score The relevance score
     * @param scoreType The type of score (e.g., "TF-IDF", "BM25")
     */
    public DocumentScore(Document document, double score, String scoreType) {
        if (document == null) {
            throw new IllegalArgumentException("Document cannot be null");
        }
        this.document = document;
        this.score = score;
        this.scoreType = scoreType;
    }
    
    /**
     * Gets the document.
     * 
     * @return The document
     */
    public Document getDocument() {
        return document;
    }
    
    /**
     * Gets the relevance score.
     * 
     * @return The score
     */
    public double getScore() {
        return score;
    }
    
    /**
     * Gets the score type.
     * 
     * @return The score type
     */
    public String getScoreType() {
        return scoreType;
    }
    
    /**
     * Compares this document score with another document score.
     * Documents are compared by score in descending order (higher scores first).
     * 
     * @param other The other document score to compare with
     * @return A negative integer, zero, or a positive integer as this score
     *         is greater than, equal to, or less than the specified score
     */
    @Override
    public int compareTo(DocumentScore other) {
        // Compare in reverse order (higher scores first)
        return Double.compare(other.score, this.score);
    }
    
    @Override
    public String toString() {
        return String.format("DocumentScore{document=%s, score=%.4f, type=%s}", 
                document.getId(), score, scoreType);
    }
}
