package com.IR.SearchEngine.evaluation;

import java.util.List;
import java.util.Set;

/**
 * Provides various metrics for evaluating search engine performance.
 * This class implements standard IR evaluation metrics like precision, recall, F-measure, etc.
 * 
 * Responsibilities:
 * - Calculate precision, recall, and F-measure at different cutoff points
 * - Compute Mean Average Precision (MAP)
 * - Generate evaluation reports and summaries (though reports are handled by Evaluator)
 * - Support comparison between different retrieval models (facilitated by Evaluator)
 * 
 * Implementation notes:
 * - Should handle edge cases (empty result sets, etc.)
 * - May provide visualization methods for evaluation results (not implemented here)
 * 
 * @author alexhere
 */
public class EvaluationMetrics {

    /**
     * Calculates Precision at K.
     * Precision@K = (Number of relevant documents in top K) / K
     *
     * @param rankedDocIds   List of retrieved document IDs, ordered by rank.
     * @param relevantDocIds Set of true relevant document IDs.
     * @param k              The cutoff value.
     * @return Precision at K.
     */
    public double calculatePrecisionAtK(List<String> rankedDocIds, Set<String> relevantDocIds, int k) {
        if (k <= 0 || rankedDocIds == null || rankedDocIds.isEmpty()) {
            return 0.0;
        }

        List<String> topKDocs = rankedDocIds.subList(0, Math.min(k, rankedDocIds.size()));
        long relevantRetrievedCount = topKDocs.stream().filter(relevantDocIds::contains).count();

        return (double) relevantRetrievedCount / Math.min(k, rankedDocIds.size()); // Denominator is min(k, |retrieved|)
    }

    /**
     * Calculates Recall at K.
     * Recall@K = (Number of relevant documents in top K) / (Total number of relevant documents)
     *
     * @param rankedDocIds   List of retrieved document IDs, ordered by rank.
     * @param relevantDocIds Set of true relevant document IDs.
     * @param k              The cutoff value.
     * @return Recall at K.
     */
    public double calculateRecallAtK(List<String> rankedDocIds, Set<String> relevantDocIds, int k) {
        if (relevantDocIds == null || relevantDocIds.isEmpty()) {
            // If there are no true relevant documents:
            // - If we also retrieved nothing, recall could be seen as 1 (perfectly recalled nothing).
            // - If we retrieved something, recall is 0 (retrieved items when none were relevant).
            return (rankedDocIds == null || rankedDocIds.isEmpty()) ? 1.0 : 0.0; 
        }
        if (k <= 0 || rankedDocIds == null || rankedDocIds.isEmpty()) {
            return 0.0;
        }

        List<String> topKDocs = rankedDocIds.subList(0, Math.min(k, rankedDocIds.size()));
        long relevantRetrievedCount = topKDocs.stream().filter(relevantDocIds::contains).count();

        return (double) relevantRetrievedCount / relevantDocIds.size();
    }

    /**
     * Calculates the F1-Score.
     * F1 = 2 * (Precision * Recall) / (Precision + Recall)
     *
     * @param precision The precision value.
     * @param recall    The recall value.
     * @return The F1-Score. Returns 0 if precision + recall is 0.
     */
    public double calculateF1Score(double precision, double recall) {
        if (precision + recall == 0) {
            return 0.0;
        }
        return 2 * (precision * recall) / (precision + recall);
    }

    /**
     * Calculates Average Precision (AP) for a single query.
     * AP = sum (P@i * rel(i)) / (Number of relevant documents)
     * where P@i is precision at rank i, and rel(i) is 1 if doc at rank i is relevant, 0 otherwise.
     *
     * @param rankedDocIds   List of retrieved document IDs, ordered by rank.
     * @param relevantDocIds Set of true relevant document IDs.
     * @return Average Precision.
     */
    public double calculateAveragePrecision(List<String> rankedDocIds, Set<String> relevantDocIds) {
        if (rankedDocIds == null || rankedDocIds.isEmpty() || relevantDocIds == null || relevantDocIds.isEmpty()) {
            return 0.0;
        }

        double sumOfPrecisions = 0.0;
        int relevantDocsFound = 0;
        int numRetrieved = rankedDocIds.size(); // Use the size of the input list

        for (int i = 0; i < numRetrieved; i++) { // Iterate directly over rankedDocIds
            String docId = rankedDocIds.get(i);
            if (relevantDocIds.contains(docId)) {
                relevantDocsFound++;
                // Precision at current rank i+1
                double precisionAtI = (double) relevantDocsFound / (i + 1);
                sumOfPrecisions += precisionAtI;
            }
        }

        if (relevantDocsFound == 0) { 
            return 0.0;
        }
        
        // Divide by the total number of *true* relevant documents for this query
        return sumOfPrecisions / relevantDocIds.size();
    }

    /**
     * Calculates Mean Average Precision (MAP).
     * MAP = (sum of Average Precisions for all queries) / (Number of queries)
     *
     * @param averagePrecisions List of Average Precision scores for each query.
     * @return Mean Average Precision.
     */
    public double calculateMAP(List<Double> averagePrecisions) {
        if (averagePrecisions == null || averagePrecisions.isEmpty()) {
            return 0.0;
        }
        return averagePrecisions.stream().mapToDouble(Double::doubleValue).average().orElse(0.0);
    }
}
