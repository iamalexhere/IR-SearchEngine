/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package com.IR.SearchEngine.data;

import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;
import java.nio.file.Path;

/**
 * Represents a document in the search engine.
 * This class encapsulates all information related to a single document in the corpus.
 * 
 * Responsibilities:
 * - Store document metadata (ID, title, etc.)
 * - Maintain the original and processed content of the document
 * - Provide access to document statistics (length, term frequencies, etc.)
 * - Support serialization for index persistence
 * 
 * Implementation notes:
 * - Should be immutable after initial processing
 * - Will be used extensively by the indexing and retrieval components
 * 
 * @author alexhere
 */
public class Document implements Serializable {
    
    private static final long serialVersionUID = 1L;
    
    private final String id;              // Unique identifier for the document
    private final String title;           // Document title
    private final String originalContent;  // Original unprocessed content
    private String processedContent;       // Content after preprocessing
    private Path filePath;                // Path to the source file (if applicable)
    private Map<String, Integer> termFrequencies; // Term frequency map for this document
    private int length;                   // Document length (in terms)
    
    /**
     * Constructor for creating a document with original content.
     * 
     * @param id Unique identifier for the document
     * @param title Document title
     * @param content Original document content
     */
    public Document(String id, String title, String content) {
        this.id = id;
        this.title = title;
        this.originalContent = content;
        this.termFrequencies = new HashMap<>();
        this.length = 0;
    }
    
    /**
     * Constructor for creating a document from a file.
     * 
     * @param id Unique identifier for the document
     * @param title Document title
     * @param content Original document content
     * @param filePath Path to the source file
     */
    public Document(String id, String title, String content, Path filePath) {
        this(id, title, content);
        this.filePath = filePath;
    }
    
    /**
     * Sets the processed content after preprocessing.
     * 
     * @param processedContent Processed content
     */
    public void setProcessedContent(String processedContent) {
        this.processedContent = processedContent;
    }
    
    /**
     * Updates the term frequency map and document length.
     * 
     * @param termFrequencies Map of terms to their frequencies
     */
    public void setTermFrequencies(Map<String, Integer> termFrequencies) {
        this.termFrequencies = termFrequencies;
        this.length = termFrequencies.values().stream().mapToInt(Integer::intValue).sum();
    }
    
    /**
     * Gets the document ID.
     * 
     * @return Document ID
     */
    public String getId() {
        return id;
    }
    
    /**
     * Gets the document title.
     * 
     * @return Document title
     */
    public String getTitle() {
        return title;
    }
    
    /**
     * Gets the original unprocessed content.
     * 
     * @return Original content
     */
    public String getOriginalContent() {
        return originalContent;
    }
    
    /**
     * Gets the processed content.
     * 
     * @return Processed content
     */
    public String getProcessedContent() {
        return processedContent;
    }
    
    /**
     * Gets the path to the source file.
     * 
     * @return File path
     */
    public Path getFilePath() {
        return filePath;
    }
    
    /**
     * Gets the term frequency map.
     * 
     * @return Map of terms to their frequencies
     */
    public Map<String, Integer> getTermFrequencies() {
        return termFrequencies;
    }
    
    /**
     * Gets the frequency of a specific term in this document.
     * 
     * @param term Term to get frequency for
     * @return Term frequency or 0 if term not found
     */
    public int getTermFrequency(String term) {
        return termFrequencies.getOrDefault(term, 0);
    }
    
    /**
     * Gets the document length (sum of all term frequencies).
     * 
     * @return Document length
     */
    public int getLength() {
        return length;
    }
    
    @Override
    public String toString() {
        return "Document{" +
                "id='" + id + '\'' +
                ", title='" + title + '\'' +
                ", length=" + length +
                '}';
    }
}
