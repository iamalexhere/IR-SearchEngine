/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package com.IR.SearchEngine.data;

import java.util.List;

/**
 * Represents a posting in the inverted index.
 * A posting contains information about a term's occurrence in a specific document.
 * 
 * Responsibilities:
 * - Store document ID where the term occurs
 * - Maintain term frequency and positional information
 * - Support efficient storage and retrieval in the inverted index
 * - Provide methods for scoring calculations
 * 
 * Implementation notes:
 * - Should be optimized for memory efficiency
 * - May contain additional information like term positions for phrase queries
 * 
 * @author alexhere updated by feliks
 */
public class Posting {

    /**
     * Constructs a Posting object with the specified parameters.
     * 
     * @author feliks
     * 
     * @param docId The document ID where the term occurs
     * @param termFrequency The frequency of the term in the document
     * @param positions The positions of the term in the document
     * @return A new Posting object
     */
    
    private final int docId; 
    private int termFrequency; 
    private List<Integer> positions;
    
    public Posting(int docId, int termFrequency, List<Integer> positions) {
        this.docId = docId;
        this.termFrequency = termFrequency;
        this.positions = positions;
    }

    public int getDocId() {
        return docId;
    }

    public int getTermFrequency() {
        return termFrequency;
    }

    public List<Integer> getPositions() {
        return positions;
    } 

    public void incrementFrequency() {
        this.termFrequency++;
    }

}
