package com.IR.SearchEngine.evaluation;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Represents the ground truth data used for evaluation.
 * This class manages relevance judgments for query-document pairs.
 * 
 * Responsibilities:
 * - Load and parse relevance judgments from standard formats (TREC, etc.)
 * - Provide access to relevance information for evaluation metrics
 * - Support different relevance scales (binary, graded)
 * - Handle missing judgments appropriately
 * 
 * Implementation notes:
 * - Should efficiently store and retrieve relevance judgments
 * - May support pooling for incomplete judgments
 * 
 * @author alexhere
 */
public class GroundTruth {

    // Stores relevance judgments: QueryID -> (DocumentID -> RelevanceScore)
    private Map<String, Map<String, Integer>> relevanceJudgments;

    public GroundTruth() {
        this.relevanceJudgments = new HashMap<>();
    }

    /**
     * Adds a relevance judgment.
     *
     * @param queryId The ID of the query.
     * @param docId The ID of the document.
     * @param relevance The relevance score (e.g., 0 for non-relevant, 1+ for relevant).
     */
    public void addRelevance(String queryId, String docId, int relevance) {
        this.relevanceJudgments.computeIfAbsent(queryId, k -> new HashMap<>()).put(docId, relevance);
    }

    /**
     * Gets the relevance score of a document for a given query.
     *
     * @param queryId The ID of the query.
     * @param docId The ID of the document.
     * @return The relevance score. Returns 0 if no judgment exists (assuming non-relevant if not specified).
     */
    public int getRelevance(String queryId, String docId) {
        return this.relevanceJudgments.getOrDefault(queryId, new HashMap<>()).getOrDefault(docId, 0);
    }

    /**
     * Checks if a document is considered relevant for a query.
     * Assumes relevance > 0 means relevant.
     *
     * @param queryId The ID of the query.
     * @param docId The ID of the document.
     * @return true if the document is relevant, false otherwise.
     */
    public boolean isRelevant(String queryId, String docId) {
        return getRelevance(queryId, docId) > 0;
    }
    
    /**
     * Gets all relevant document IDs for a given query.
     *
     * @param queryId The ID of the query.
     * @return A set of document IDs considered relevant for the query. Returns an empty set if the query is not found or has no relevant documents.
     */
    public Set<String> getRelevantDocuments(String queryId) {
        Map<String, Integer> queryJudgments = this.relevanceJudgments.get(queryId);
        if (queryJudgments == null) {
            return Set.of(); // Return an empty set if the query ID is not found
        }
        
        Set<String> relevantDocs = queryJudgments.entrySet().stream()
            .filter(entry -> entry.getValue() > 0)
            .map(Map.Entry::getKey)
            .collect(Collectors.toSet());
            
        return relevantDocs;
    }


    /**
     * Loads relevance judgments from a file.
     * Expected format: queryId<TAB>docId<TAB>relevanceScore per line.
     * (TREC qrels format is typically: queryId 0 docId relevance)
     * This method attempts to parse lines assuming at least 3 parts, where the first is queryId,
     * the second-to-last is docId, and the last is relevance score.
     *
     * @param filePath The path to the ground truth file.
     * @throws IOException If an error occurs while reading the file.
     */
    public void loadFromFile(Path filePath) throws IOException {
        try (BufferedReader reader = new BufferedReader(new FileReader(filePath.toFile()))) {
            String line;
            while ((line = reader.readLine()) != null) {
                if (line.trim().isEmpty() || line.trim().startsWith("#")) {
                    continue; // Skip empty lines and comments
                }
                String[] parts = line.trim().split("\\s+"); // Split by one or more whitespace characters

                if (parts.length >= 3) { 
                    String queryId = parts[0];
                    String relevanceStr = parts[parts.length - 1];
                    
                    int docIdStartIndex = 1; 
                    if (parts.length > 3 && (parts[1].equalsIgnoreCase("Q0") || parts[1].matches("^\\d+$"))) {
                        docIdStartIndex = 2;
                    }

                    int docIdEndIndex = parts.length - 2;

                    if (docIdStartIndex <= docIdEndIndex) {
                        StringBuilder docIdBuilder = new StringBuilder();
                        for (int i = docIdStartIndex; i <= docIdEndIndex; i++) {
                            docIdBuilder.append(parts[i]);
                            if (i < docIdEndIndex) {
                                docIdBuilder.append(" "); 
                            }
                        }
                        String docId = docIdBuilder.toString();

                        try {
                            int relevance = Integer.parseInt(relevanceStr);
                            addRelevance(queryId, docId, relevance);
                        } catch (NumberFormatException e) {
                            System.err.println("Skipping line due to invalid relevance score: " + line + " (Could not parse '" + relevanceStr + "')");
                        }
                    } else {
                         System.err.println("Skipping malformed line (cannot determine document ID from parts): " + line);
                    }
                } else { 
                    System.err.println("Skipping malformed line in ground truth file (not enough parts - need at least 3): " + line);
                }
            }
        }
        System.out.println("Loaded " + relevanceJudgments.size() + " queries' judgments from " + filePath.toString());
        // For debugging, print number of judgments for a few queries
        relevanceJudgments.entrySet().stream().limit(5).forEach(entry -> 
            System.out.println("Query " + entry.getKey() + " has " + entry.getValue().size() + " judgments.")
        );
    }
    
    /**
     * Returns the set of all query IDs for which there are relevance judgments.
     * @return A set of query IDs.
     */
    public Set<String> getAllQueryIds() {
        return this.relevanceJudgments.keySet();
    }
}
