/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package com.IR.SearchEngine.preprocessing;

import com.IR.SearchEngine.data.Document;
import opennlp.tools.stemmer.PorterStemmer;
import opennlp.tools.tokenize.SimpleTokenizer;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

/**
 * Implements text preprocessing operations for the search engine.
 * This class handles various text normalization and analysis tasks.
 * 
 * Responsibilities:
 * - Perform tokenization, stemming, and stopword removal
 * - Handle text normalization (case folding, punctuation removal)
 * - Support language detection and language-specific processing
 * - Provide methods for both document and query preprocessing
 * 
 * Implementation notes:
 * - Uses OpenNLP for advanced NLP tasks
 * - Supports configurable preprocessing pipeline
 * - Implements caching for efficiency
 * 
 * @author alexhere
 */
public class Preprocessor implements IPreprocessor {
    
    private final SimpleTokenizer tokenizer;
    private final PorterStemmer stemmer;
    private final Set<String> stopwords;
    private final Map<String, String[]> tokenCache;
    private final Map<String, String> stemCache;
    
    private boolean applyStemming;
    private boolean removeStopwords;
    
    /**
     * Default constructor that initializes the preprocessor with default settings.
     */
    public Preprocessor() {
        this.tokenizer = SimpleTokenizer.INSTANCE;
        this.stemmer = new PorterStemmer();
        this.stopwords = new HashSet<>();
        this.tokenCache = new ConcurrentHashMap<>();
        this.stemCache = new ConcurrentHashMap<>();
        this.applyStemming = true;
        this.removeStopwords = true;
        
        // Load default English stopwords
        loadDefaultStopwords();
    }
    
    /**
     * Loads default English stopwords from a resource file.
     */
    private void loadDefaultStopwords() {
        // Common English stopwords
        String[] defaultStopwords = {
            "a", "an", "and", "are", "as", "at", "be", "but", "by", "for", "if", "in", "into",
            "is", "it", "no", "not", "of", "on", "or", "such", "that", "the", "their", "then", "there",
            "these", "they", "this", "to", "was", "will", "with"
        };
        
        Collections.addAll(stopwords, defaultStopwords);
        
        // Try to load more comprehensive stopwords from resource file if available
        try (InputStream is = getClass().getResourceAsStream("/stopwords-en.txt");
             BufferedReader reader = new BufferedReader(new InputStreamReader(is))) {
            
            String line;
            while ((line = reader.readLine()) != null) {
                line = line.trim();
                if (!line.isEmpty() && !line.startsWith("#")) {
                    stopwords.add(line.toLowerCase());
                }
            }
        } catch (IOException | NullPointerException e) {
            // Resource file not found, continue with default stopwords
            System.out.println("Warning: Could not load stopwords file. Using default stopwords.");
        }
    }
    
    @Override
    public Document preprocessDocument(Document document) {
        String originalContent = document.getOriginalContent();
        if (originalContent == null || originalContent.isEmpty()) {
            return document;
        }
        
        // Normalize and tokenize the text
        String normalizedText = normalizeText(originalContent);
        String[] tokens = tokenize(normalizedText);
        
        // Apply stopword removal if enabled
        if (removeStopwords) {
            tokens = removeStopwords(tokens);
        }
        
        // Apply stemming if enabled
        if (applyStemming) {
            tokens = stem(tokens);
        }
        
        // Compute term frequencies
        Map<String, Integer> termFrequencies = computeTermFrequencies(tokens);
        
        // Update the document with processed content and term frequencies
        String processedContent = String.join(" ", tokens);
        document.setProcessedContent(processedContent);
        document.setTermFrequencies(termFrequencies);
        
        return document;
    }
    
    @Override
    public List<Document> preprocessDocuments(List<Document> documents) {
        return documents.stream()
                .map(this::preprocessDocument)
                .collect(Collectors.toList());
    }
    
    @Override
    public String preprocessQuery(String query) {
        if (query == null || query.isEmpty()) {
            return "";
        }
        
        // Normalize and tokenize the query
        String normalizedText = normalizeText(query);
        String[] tokens = tokenize(normalizedText);
        
        // Apply stopword removal if enabled
        if (removeStopwords) {
            tokens = removeStopwords(tokens);
        }
        
        // Apply stemming if enabled
        if (applyStemming) {
            tokens = stem(tokens);
        }
        
        // Join tokens back into a string
        return String.join(" ", tokens);
    }
    
    @Override
    public String[] tokenize(String text) {
        // Check cache first
        if (tokenCache.containsKey(text)) {
            return tokenCache.get(text);
        }
        
        // Use OpenNLP tokenizer
        String[] tokens = tokenizer.tokenize(text);
        
        // Cache the result
        tokenCache.put(text, tokens);
        
        return tokens;
    }
    
    @Override
    public String[] removeStopwords(String[] tokens) {
        if (tokens == null || tokens.length == 0) {
            return new String[0];
        }
        
        return Arrays.stream(tokens)
                .filter(token -> !stopwords.contains(token.toLowerCase()))
                .toArray(String[]::new);
    }
    
    @Override
    public String[] stem(String[] tokens) {
        if (tokens == null || tokens.length == 0) {
            return new String[0];
        }
        
        return Arrays.stream(tokens)
                .map(this::stemWord)
                .toArray(String[]::new);
    }
    
    /**
     * Stems a single word using Porter stemmer with caching.
     * 
     * @param word Word to stem
     * @return Stemmed word
     */
    private String stemWord(String word) {
        // Check cache first
        if (stemCache.containsKey(word)) {
            return stemCache.get(word);
        }
        
        // Apply Porter stemmer
        String stemmed = stemmer.stem(word);
        
        // Cache the result
        stemCache.put(word, stemmed);
        
        return stemmed;
    }
    
    @Override
    public Map<String, Integer> computeTermFrequencies(String[] tokens) {
        if (tokens == null || tokens.length == 0) {
            return Collections.emptyMap();
        }
        
        Map<String, Integer> termFrequencies = new HashMap<>();
        
        for (String token : tokens) {
            termFrequencies.put(token, termFrequencies.getOrDefault(token, 0) + 1);
        }
        
        return termFrequencies;
    }
    
    @Override
    public String normalizeText(String text) {
        if (text == null || text.isEmpty()) {
            return "";
        }
        
        // Convert to lowercase
        String normalized = text.toLowerCase();
        
        // Remove punctuation and special characters, keeping only letters, numbers, and spaces
        normalized = normalized.replaceAll("[^a-z0-9\\s]", "");
        
        // Replace multiple spaces with a single space
        normalized = normalized.replaceAll("\\s+", " ").trim();
        
        return normalized;
    }
    
    @Override
    public void addStopword(String stopword) {
        if (stopword != null && !stopword.isEmpty()) {
            stopwords.add(stopword.toLowerCase());
        }
    }
    
    @Override
    public void setApplyStemming(boolean applyStemming) {
        this.applyStemming = applyStemming;
    }
    
    @Override
    public void setRemoveStopwords(boolean removeStopwords) {
        this.removeStopwords = removeStopwords;
    }
    
    /**
     * Clears the token and stem caches to free up memory.
     */
    public void clearCaches() {
        tokenCache.clear();
        stemCache.clear();
    }
    
    /**
     * Adds multiple stopwords at once.
     * 
     * @param newStopwords Collection of stopwords to add
     */
    public void addStopwords(Collection<String> newStopwords) {
        if (newStopwords != null) {
            newStopwords.forEach(this::addStopword);
        }
    }
    
    /**
     * Gets the current set of stopwords.
     * 
     * @return Unmodifiable set of stopwords
     */
    public Set<String> getStopwords() {
        return Collections.unmodifiableSet(stopwords);
    }
    
    /**
     * Checks if stemming is currently enabled.
     * 
     * @return True if stemming is enabled, false otherwise
     */
    public boolean isApplyStemming() {
        return applyStemming;
    }
    
    /**
     * Checks if stopword removal is currently enabled.
     * 
     * @return True if stopword removal is enabled, false otherwise
     */
    public boolean isRemoveStopwords() {
        return removeStopwords;
    }
}
