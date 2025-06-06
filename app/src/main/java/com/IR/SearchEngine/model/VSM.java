/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package com.IR.SearchEngine.model;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.Comparator;
import java.util.List;
import java.util.ArrayList;
import java.lang.Math; // Import Math class

/**
 * Implements the Vector Space Model for document retrieval.
 * VSM represents documents and queries as vectors in a high-dimensional space.
 *
 * Responsibilities:
 * - Calculate TF-IDF weights for terms in documents and queries
 * - Compute cosine similarity between document and query vectors
 * - Support different term weighting schemes (binary, TF, TF-IDF) - Current implementation uses TF-IDF
 * - Handle document length normalization - Implicit in cosine similarity denominator
 *
 * Implementation notes:
 * - Uses HashMap for sparse vector representation
 * - Basic tokenization by splitting on spaces
 *
 * @author alexhere
 */
public class VSM {

    // Stores TF-IDF vector for each document: Document ID -> Term -> TF-IDF Weight
    private Map<String, Map<String, Double>> tfidfVectors;

    // Stores Document Frequency for each term: Term -> Number of documents containing the term
    private Map<String, Integer> documentFrequency;

    // Stores the inverse document frequency for each term: Term -> IDF
    private Map<String, Double> inverseDocumentFrequency;

    // Total number of documents in the corpus
    private int totalDocuments;

    // Stores raw term frequencies for each document: Document ID -> Term -> Frequency
    private Map<String, Map<String, Integer>> termFrequencies;


    public VSM() {
        this.tfidfVectors = new HashMap<>();
        this.documentFrequency = new HashMap<>();
        this.inverseDocumentFrequency = new HashMap<>();
        this.termFrequencies = new HashMap<>();
        this.totalDocuments = 0;
    }

    /**
     * Builds the VSM index from a collection of documents.
     *
     * @param documents A map where keys are document IDs and values are the document content.
     */
    public void buildIndex(Map<String, String> documents) {
        this.totalDocuments = documents.size();

        // 1. Calculate Term Frequencies (TF) for each document
        calculateTermFrequencies(documents);

        // 2. Calculate Document Frequencies (DF)
        calculateDocumentFrequencies();

        // 3. Calculate Inverse Document Frequencies (IDF)
        calculateInverseDocumentFrequencies();

        // 4. Calculate TF-IDF weights for each document
        calculateTfidfVectors();
    }

    /**
     * Calculates the term frequencies for each document.
     *
     * @param documents The map of documents.
     */
    private void calculateTermFrequencies(Map<String, String> documents) {
        for (Map.Entry<String, String> entry : documents.entrySet()) {
            String docId = entry.getKey();
            String content = entry.getValue();

            Map<String, Integer> tf = new HashMap<>();
            // Basic tokenization: split by spaces and convert to lowercase
            String[] terms = content.toLowerCase().split("\\s+");

            for (String term : terms) {
                tf.put(term, tf.getOrDefault(term, 0) + 1);
            }
            termFrequencies.put(docId, tf);
        }
    }

    /**
     * Calculates the document frequencies for each term across the corpus.
     */
    private void calculateDocumentFrequencies() {
        for (Map<String, Integer> tf : termFrequencies.values()) {
            Set<String> uniqueTermsInDoc = new HashSet<>(tf.keySet());
            for (String term : uniqueTermsInDoc) {
                documentFrequency.put(term, documentFrequency.getOrDefault(term, 0) + 1);
            }
        }
    }

    /**
     * Calculates the inverse document frequencies for each term.
     */
    private void calculateInverseDocumentFrequencies() {
        for (String term : documentFrequency.keySet()) {
            double df = documentFrequency.get(term);
            // Add 1 to denominator to avoid division by zero for terms not in corpus (shouldn't happen here)
            // and use log base 10 or natural log - here using natural log.
            double idf = Math.log(totalDocuments / (df));
            inverseDocumentFrequency.put(term, idf);
        }
    }

    /**
     * Calculates the TF-IDF vectors for all documents.
     */
    private void calculateTfidfVectors() {
        for (Map.Entry<String, Map<String, Integer>> docEntry : termFrequencies.entrySet()) {
            String docId = docEntry.getKey();
            Map<String, Integer> tf = docEntry.getValue();
            Map<String, Double> tfidf = new HashMap<>();

            for (Map.Entry<String, Integer> termEntry : tf.entrySet()) {
                String term = termEntry.getKey();
                int termFreq = termEntry.getValue();
                double idf = inverseDocumentFrequency.getOrDefault(term, 0.0); // Get IDF, default to 0 if term not in IDF map

                double tfidfWeight = (double) termFreq * idf;
                tfidf.put(term, tfidfWeight);
            }
            tfidfVectors.put(docId, tfidf);
        }
    }


    /**
     * Calculates the TF-IDF vector for a given query.
     * Query vector calculation usually uses raw TF or slightly different weighting.
     * This uses a simple TF-IDF where IDF is based on the document corpus.
     *
     * @param query The search query string.
     * @return The TF-IDF vector for the query.
     */
    public Map<String, Double> getQueryVector(String query) {
        Map<String, Integer> queryTf = new HashMap<>();
        // Basic tokenization: split by spaces and convert to lowercase
        String[] terms = query.toLowerCase().split("\\s+");

        for (String term : terms) {
            queryTf.put(term, queryTf.getOrDefault(term, 0) + 1);
        }

        Map<String, Double> queryVector = new HashMap<>();
        for (Map.Entry<String, Integer> entry : queryTf.entrySet()) {
            String term = entry.getKey();
            int termFreq = entry.getValue();
            // Use IDF from the document corpus
            double idf = inverseDocumentFrequency.getOrDefault(term, 0.0);

            double tfidfWeight = (double) termFreq * idf;
            queryVector.put(term, tfidfWeight);
        }
        return queryVector;
    }


    /**
     * Calculates the cosine similarity between two vectors.
     *
     * @param vector1 The first vector (e.g., query vector).
     * @param vector2 The second vector (e.g., document vector).
     * @return The cosine similarity score (between 0 and 1).
     */
    public double cosineSimilarity(Map<String, Double> vector1, Map<String, Double> vector2) {
        double dotProduct = 0.0;
        double magnitude1 = 0.0;
        double magnitude2 = 0.0;

        // Calculate dot product
        for (Map.Entry<String, Double> entry : vector1.entrySet()) {
            String term = entry.getKey();
            Double weight1 = entry.getValue();
            Double weight2 = vector2.get(term); // Get weight from vector2

            if (weight2 != null) {
                dotProduct += weight1 * weight2;
            }
        }

        // Calculate magnitude of vector1
        for (Double weight : vector1.values()) {
            magnitude1 += weight * weight;
        }
        magnitude1 = Math.sqrt(magnitude1);

        // Calculate magnitude of vector2
        for (Double weight : vector2.values()) {
            magnitude2 += weight * weight;
        }
        magnitude2 = Math.sqrt(magnitude2);

        // Calculate cosine similarity
        if (magnitude1 == 0 || magnitude2 == 0) {
            return 0.0; // Avoid division by zero
        } else {
            return dotProduct / (magnitude1 * magnitude2);
        }
    }

    /**
     * Searches the indexed documents for the given query and returns a ranked list of document IDs.
     *
     * @param query The search query string.
     * @return A list of document IDs ranked by relevance (highest similarity first).
     */
    public List<String> search(String query) {
        Map<String, Double> queryVector = getQueryVector(query);
        Map<String, Double> similarityScores = new HashMap<>();

        for (Map.Entry<String, Map<String, Double>> docEntry : tfidfVectors.entrySet()) {
            String docId = docEntry.getKey();
            Map<String, Double> documentVector = docEntry.getValue();
            double similarity = cosineSimilarity(queryVector, documentVector);
            similarityScores.put(docId, similarity);
        }

        // Sort documents by similarity score in descending order
        List<Map.Entry<String, Double>> sortedDocuments = new ArrayList<>(similarityScores.entrySet());
        sortedDocuments.sort(Map.Entry.comparingByValue(Comparator.reverseOrder()));

        // Extract ranked document IDs
        List<String> rankedDocIds = new ArrayList<>();
        for (Map.Entry<String, Double> entry : sortedDocuments) {
            // Optionally, you could filter out documents with similarity 0
             if (entry.getValue() > 0) {
                rankedDocIds.add(entry.getKey());
            }
        }

        return rankedDocIds;
    }


    public static void main(String[] args) {
        // Example usage:
        VSM vsm = new VSM();

        Map<String, String> documents = new HashMap<>();
        documents.put("doc1", "This is the first document about information retrieval.");
        documents.put("doc2", "This document is about vector space models.");
        documents.put("doc3", "Information retrieval is an interesting topic.");
        documents.put("doc4", "Vector space models are used in information retrieval.");

        vsm.buildIndex(documents);

        String query = "information retrieval";
        List<String> results = vsm.search(query);

        System.out.println("Search results for query: \"" + query + "\"");
        if (results.isEmpty()) {
            System.out.println("No documents found.");
        } else {
            System.out.println("Ranked document IDs:");
            for (String docId : results) {
                System.out.println("- " + docId);
            }
        }

        query = "vector models";
         results = vsm.search(query);

        System.out.println("\nSearch results for query: \"" + query + "\"");
        if (results.isEmpty()) {
            System.out.println("No documents found.");
        } else {
            System.out.println("Ranked document IDs:");
            for (String docId : results) {
                System.out.println("- " + docId);
            }
        }
    }
}
