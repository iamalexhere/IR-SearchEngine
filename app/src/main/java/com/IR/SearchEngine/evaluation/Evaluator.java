package com.IR.SearchEngine.evaluation;

import com.IR.SearchEngine.app.App;
import com.IR.SearchEngine.data.DocumentScore;
import com.IR.SearchEngine.data.QueryResult;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Coordinates the evaluation process for the search engine.
 * This class manages the evaluation workflow and uses EvaluationMetrics for calculations.
 * 
 * Responsibilities:
 * - Run evaluation experiments with different configurations
 * - Coordinate the comparison of retrieval models
 * - Generate comprehensive evaluation reports
 * - Utilize ground truth data for evaluation
 * 
 * Implementation notes:
 * - Should support batch evaluation of multiple queries
 * - May implement cross-validation for parameter tuning
 * 
 * @author alexhere
 */
public class Evaluator {

    private final App searchApp; // To execute queries
    private final GroundTruth groundTruth;
    private final Map<String, String> queryIdToTextMap;
    private EvaluationMetrics metricsCalculator;

    /**
     * Constructor for the Evaluator.
     *
     * @param searchApp   The search application instance to execute queries.
     * @param groundTruth The ground truth data.
     * @param queryIdToTextMap A map from query ID (from qrels) to actual query text.
     */
    public Evaluator(App searchApp, GroundTruth groundTruth, Map<String, String> queryIdToTextMap) {
        this.searchApp = searchApp;
        this.groundTruth = groundTruth;
        this.queryIdToTextMap = queryIdToTextMap;
        this.metricsCalculator = new EvaluationMetrics(); // Instantiate or use static methods
    }

    /**
     * Evaluates the current model in the App for a given set of query IDs from the ground truth.
     *
     * @param queryIds The set of query IDs to evaluate.
     * @param topK     The number of results to consider for each query (e.g., for P@K, R@K).
     * @return A map where keys are query IDs and values are maps of metric names to their scores.
     */
    public Map<String, Map<String, Double>> evaluateQueries(Set<String> queryIds, int topK) {
        Map<String, Map<String, Double>> allQueryResults = new HashMap<>();

        if (searchApp == null) {
            System.err.println("SearchApp is not initialized in Evaluator. Cannot run queries.");
            return allQueryResults;
        }
        if (groundTruth == null) {
            System.err.println("GroundTruth is not initialized in Evaluator. Cannot evaluate.");
            return allQueryResults;
        }

        for (String queryId : queryIds) {
            String queryText = queryIdToTextMap.get(queryId);

            if (queryText == null) {
                System.err.println("Warning: Query ID '" + queryId + "' from qrels file not found in loaded query files (map). Skipping evaluation for this query.");
                allQueryResults.put(queryId, new HashMap<>()); // Record that we attempted it, but metrics will be 0 or absent
                continue;
            }

            System.out.println("Evaluating query ID: " + queryId + " (Using mapped query text: \"" + queryText + "\")");

            QueryResult queryResult = searchApp.executeQuery(queryText, topK);
            
            List<String> rankedDocIds = (queryResult != null && queryResult.getResults() != null) ? 
                                        queryResult.getResults().stream()
                                                   .map(ds -> ds.getDocument().getId())
                                                   .collect(Collectors.toList()) :
                                        List.of();

            if (queryResult == null || rankedDocIds.isEmpty()) {
                 System.out.println("No results returned for query ID: " + queryId + ". Metrics will be 0.");
            }
            
            Set<String> relevantDocIdsFromGT = groundTruth.getRelevantDocuments(queryId);

            System.out.println("\n---------------------------------------------------------------------------");
            System.out.println("[DEBUG] EVALUATING Query ID: \"" + queryId + "\"");
            System.out.println("[DEBUG] Using Query Text   : \"" + queryText + "\"");
            System.out.println("[DEBUG] Requested Top K    : " + topK);
            System.out.println("---------------------------------------------------------------------------");

            System.out.println("[DEBUG] --- Model's Retrieved Documents (up to Top " + topK + ") ---");
            if (queryResult == null || queryResult.getResults() == null || queryResult.getResults().isEmpty()) {
                System.out.println("[DEBUG] Model returned no results for this query.");
            } else {
                List<DocumentScore> modelResults = queryResult.getResults(); // This list is already top K or less
                for (int i = 0; i < modelResults.size(); i++) {
                    DocumentScore ds = modelResults.get(i);
                    System.out.printf("[DEBUG] Rank %2d: DocID=\"%s\" Score=%.4f\n", 
                                      i + 1, String.format("%-50s", "\"" + ds.getDocument().getId() + "\""), ds.getScore());
                }
            }

            System.out.println("[DEBUG] --- Ground Truth Relevant Documents (from qrels.txt) ---");
            if (relevantDocIdsFromGT.isEmpty()) {
                System.out.println("[DEBUG] No relevant documents listed in qrels.txt for this query ID.");
            } else {
                System.out.println("[DEBUG] Total relevant documents in qrels: " + relevantDocIdsFromGT.size());
                for (String relevantDocId : relevantDocIdsFromGT) {
                    int relevanceScore = groundTruth.getRelevance(queryId, relevantDocId);
                    System.out.printf("[DEBUG] - DocID=\"%s\" Relevance Score=%d\n", 
                                      String.format("%-50s", "\"" + relevantDocId + "\""), relevanceScore);
                }
            }

            System.out.println("[DEBUG] --- Matching Retrieved (Top " + topK + ") vs. Ground Truth ---");
            int relevantDocsFoundInTopK = 0;
            if (queryResult != null && queryResult.getResults() != null) {
                 List<String> actualRankedIdsInTopK = queryResult.getResults().stream()
                                                            .map(ds -> ds.getDocument().getId())
                                                            .collect(Collectors.toList());
                if (actualRankedIdsInTopK.isEmpty()) {
                     System.out.println("[DEBUG] Model returned no results, so no matches to check.");
                } else {
                    for (int i = 0; i < actualRankedIdsInTopK.size(); i++) {
                        String retrievedDocId = actualRankedIdsInTopK.get(i);
                        boolean isActuallyRelevant = relevantDocIdsFromGT.contains(retrievedDocId);
                        if (isActuallyRelevant) {
                            relevantDocsFoundInTopK++;
                        }
                        System.out.printf("[DEBUG] Rank %2d: Retrieved=\"%s\" Is in Ground Truth? %s\n", 
                                          i + 1, String.format("%-50s", "\"" + retrievedDocId + "\""), isActuallyRelevant);
                    }
                System.out.println("[DEBUG] Total relevant documents found by model in Top " + actualRankedIdsInTopK.size() + " results: " + relevantDocsFoundInTopK);
                }
            }

            // Warnings based on ground truth and results
            if (relevantDocIdsFromGT.isEmpty() && !rankedDocIds.isEmpty()) {
                System.out.println("[INFO] No relevance judgments found for query ID: " + queryId + 
                                   ", but results were returned. Metrics like Precision/Recall will be 0.");
            } else if (relevantDocIdsFromGT.isEmpty() && rankedDocIds.isEmpty()){
                 System.out.println("[INFO] No relevance judgments and no results for query ID: " + queryId + ". Metrics will be 0.");
            } else if (!relevantDocIdsFromGT.isEmpty() && rankedDocIds.isEmpty()) {
                 System.out.println("[INFO] Relevance judgments exist for query ID: " + queryId + 
                                   ", but the model returned no results. Metrics like Precision/Recall will be 0.");
            }

            System.out.println("[DEBUG] --- Calculating Metrics (K=" + topK + ") ---");
            Map<String, Double> currentQueryMetrics = new HashMap<>();

            // Calculate Precision@K
            double precisionAtK = metricsCalculator.calculatePrecisionAtK(rankedDocIds, relevantDocIdsFromGT, topK);
            currentQueryMetrics.put("Precision@" + topK, precisionAtK);
            System.out.printf("[DEBUG] Precision@%-2d = %d (relevant in top K) / %d (retrieved up to K) = %.4f\n", 
                              topK, relevantDocsFoundInTopK, Math.min(topK, rankedDocIds.size()), precisionAtK);

            // Calculate Recall@K
            double recallAtK = metricsCalculator.calculateRecallAtK(rankedDocIds, relevantDocIdsFromGT, topK);
            currentQueryMetrics.put("Recall@" + topK, recallAtK);
            System.out.printf("[DEBUG] Recall@%-5d = %d (relevant in top K) / %d (total relevant in qrels) = %.4f\n", 
                              topK, relevantDocsFoundInTopK, relevantDocIdsFromGT.size(), recallAtK);
            
            // Calculate F1-Score@K
            double f1AtK = metricsCalculator.calculateF1Score(precisionAtK, recallAtK);
            currentQueryMetrics.put("F1-Score@" + topK, f1AtK);
            System.out.printf("[DEBUG] F1-Score@%-2d = (2 * P@%d * R@%d) / (P@%d + R@%d) = %.4f\n", 
                              topK, topK, topK, topK, topK, f1AtK);

            // Calculate Average Precision (AP)
            // Note: The 'rankedDocIds' passed to calculateAveragePrecision is already limited to topK.
            // For a true AP over the entire result set, a version of executeQuery returning all results would be needed.
            // Here, we explain AP conceptually using the full list from queryResult if available.
            System.out.println("[DEBUG] Average Precision (AP@" + topK + ") Calculation Details:");
            List<String> fullRankedListFromModel = (queryResult != null && queryResult.getResults() != null) ? 
                                                  queryResult.getResults().stream().map(ds -> ds.getDocument().getId()).collect(Collectors.toList()) :
                                                  List.of();
            double sumOfPrecisionsAtRelevantRanks = 0.0;
            int relevantItemsFoundForAP = 0;
            if (!relevantDocIdsFromGT.isEmpty()) {
                for (int i = 0; i < fullRankedListFromModel.size(); i++) {
                    String docId = fullRankedListFromModel.get(i);
                    if (relevantDocIdsFromGT.contains(docId)) {
                        relevantItemsFoundForAP++;
                        double precisionAtThisRank = (double) relevantItemsFoundForAP / (i + 1);
                        sumOfPrecisionsAtRelevantRanks += precisionAtThisRank;
                        System.out.printf("[DEBUG]   Relevant doc \"%s\" found at rank %d. Precision here = %.4f. Cumulative P sum = %.4f\n", 
                                          docId, i + 1, precisionAtThisRank, sumOfPrecisionsAtRelevantRanks);
                    }
                }
                if (relevantItemsFoundForAP > 0) {
                     System.out.printf("[DEBUG]   Conceptual AP = %.4f (sum of precisions) / %d (total relevant in qrels) = %.4f\n", 
                                   sumOfPrecisionsAtRelevantRanks, relevantDocIdsFromGT.size(), sumOfPrecisionsAtRelevantRanks / relevantDocIdsFromGT.size());
                } else {
                    System.out.println("[DEBUG]   No relevant documents found in model's results, so conceptual AP is 0.");
                }
            } else {
                System.out.println("[DEBUG]   No relevant documents in ground truth, so AP is 0 (or undefined, typically 0).");
            }
            double avgPrecision = metricsCalculator.calculateAveragePrecision(rankedDocIds, relevantDocIdsFromGT); // This uses topK rankedDocIds
            currentQueryMetrics.put("AveragePrecision", avgPrecision);
            System.out.printf("[DEBUG] Calculated AP@%d (using model's top %d results for calculation) = %.4f\n", topK, topK, avgPrecision);
            
            allQueryResults.put(queryId, currentQueryMetrics);
            System.out.println("[DEBUG] --- Final Calculated Metrics for Query ID \"" + queryId + "\" ---");
            System.out.println("[DEBUG] " + currentQueryMetrics);
            System.out.println("---------------------------------------------------------------------------");
        }
        
        if (!allQueryResults.isEmpty()) {
            System.out.println("\n======================== OVERALL EVALUATION SUMMARY ========================");
            double map = metricsCalculator.calculateMAP(
                allQueryResults.values().stream()
                    .map(metrics -> metrics.getOrDefault("AveragePrecision", 0.0))
                    .collect(Collectors.toList())
            );
            System.out.printf("\nOverall Mean Average Precision (MAP): %.4f%n", map);
            
            // Calculate Mean Precision@K
            double meanPrecisionAtK = allQueryResults.values().stream()
                .mapToDouble(metrics -> metrics.getOrDefault("Precision@" + topK, 0.0))
                .average().orElse(0.0);
            System.out.printf("Mean Precision@%d: %.4f%n", topK, meanPrecisionAtK);

            // Calculate Mean Recall@K
            double meanRecallAtK = allQueryResults.values().stream()
                .mapToDouble(metrics -> metrics.getOrDefault("Recall@" + topK, 0.0))
                .average().orElse(0.0);
            System.out.printf("Mean Recall@%d: %.4f%n", topK, meanRecallAtK);
            
            // Calculate Mean F1-Score@K
             double meanF1AtK = allQueryResults.values().stream()
                .mapToDouble(metrics -> metrics.getOrDefault("F1-Score@" + topK, 0.0))
                .average().orElse(0.0);
            System.out.printf("Mean F1-Score@%d: %.4f%n", topK, meanF1AtK);

        } else {
            System.out.println("\nNo queries were evaluated, cannot calculate overall metrics.");
        }

        return allQueryResults;
    }
    
    /**
     * A simple method to run evaluation on all queries found in the ground truth.
     * @param topK The number of results to consider for each query.
     * @return A map of query IDs to their metric scores.
     */
    public Map<String, Map<String, Double>> evaluateAll(int topK) {
        if (groundTruth == null) {
            System.err.println("GroundTruth is not initialized. Cannot evaluate.");
            return new HashMap<>();
        }
        Set<String> allQueryIds = groundTruth.getAllQueryIds();
        if (allQueryIds.isEmpty()) {
            System.out.println("No query IDs found in ground truth to evaluate.");
            return new HashMap<>();
        }
        System.out.println("\nStarting evaluation for " + allQueryIds.size() + " queries from ground truth using model: " + searchApp.getCurrentModelName());
        return evaluateQueries(allQueryIds, topK);
    }
}
