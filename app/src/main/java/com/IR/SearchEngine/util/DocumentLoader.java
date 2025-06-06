/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package com.IR.SearchEngine.util;

import com.IR.SearchEngine.data.Document;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

/**
 * Responsible for loading documents from various sources.
 * This utility class handles document parsing and initial processing.
 * 
 * Responsibilities:
 * - Load documents from files, directories, or other sources
 * - Parse different document formats (text, HTML, PDF, etc.)
 * - Extract metadata from documents
 * - Convert documents to the internal representation
 * 
 * Implementation notes:
 * - Supports batch loading for efficiency
 * - Implements multithreading for parallel processing
 * - Handles various character encodings appropriately
 * 
 * @author alexhere
 */
public class DocumentLoader {
    
    private final int numThreads;
    private final ExecutorService executorService;
    
    /**
     * Default constructor that initializes the document loader with default settings.
     */
    public DocumentLoader() {
        this.numThreads = Runtime.getRuntime().availableProcessors();
        this.executorService = Executors.newFixedThreadPool(numThreads);
    }
    
    /**
     * Constructor that allows specifying the number of threads for parallel processing.
     * 
     * @param numThreads Number of threads to use
     */
    public DocumentLoader(int numThreads) {
        this.numThreads = numThreads;
        this.executorService = Executors.newFixedThreadPool(numThreads);
    }
    
    /**
     * Loads a document from a text file.
     * 
     * @param filePath Path to the text file
     * @return Document object containing the file content
     * @throws IOException If an I/O error occurs
     */
    public Document loadTextDocument(Path filePath) throws IOException {
        if (!Files.exists(filePath) || !Files.isRegularFile(filePath)) {
            throw new IOException("File does not exist or is not a regular file: " + filePath);
        }
        
        String content = Files.readString(filePath, StandardCharsets.UTF_8);
        String fileName = filePath.getFileName().toString();
        String id = fileName; // Use file name as ID for stability
        
        return new Document(id, fileName, content, filePath);
    }
    
    /**
     * Loads all text documents from a directory.
     * 
     * @param directoryPath Path to the directory containing text files
     * @return List of Document objects
     * @throws IOException If an I/O error occurs
     */
    public List<Document> loadTextDocumentsFromDirectory(Path directoryPath) throws IOException {
        if (!Files.exists(directoryPath) || !Files.isDirectory(directoryPath)) {
            throw new IOException("Directory does not exist or is not a directory: " + directoryPath);
        }
        
        List<Path> textFiles = Files.walk(directoryPath)
                .filter(Files::isRegularFile)
                .filter(path -> path.toString().endsWith(".txt"))
                .collect(Collectors.toList());
        
        return loadTextDocumentsParallel(textFiles);
    }
    
    /**
     * Loads text documents in parallel using multiple threads.
     * 
     * @param filePaths List of paths to text files
     * @return List of Document objects
     */
    private List<Document> loadTextDocumentsParallel(List<Path> filePaths) {
        List<Future<Document>> futures = new ArrayList<>();
        
        // Submit tasks to the executor service
        for (Path filePath : filePaths) {
            futures.add(executorService.submit(() -> {
                try {
                    return loadTextDocument(filePath);
                } catch (IOException e) {
                    System.err.println("Error loading document: " + filePath + ": " + e.getMessage());
                    return null;
                }
            }));
        }
        
        // Collect results
        List<Document> documents = new ArrayList<>();
        for (Future<Document> future : futures) {
            try {
                Document document = future.get();
                if (document != null) {
                    documents.add(document);
                }
            } catch (Exception e) {
                System.err.println("Error retrieving document: " + e.getMessage());
            }
        }
        
        return documents;
    }
    
    /**
     * Loads documents from a TREC format file.
     * TREC format is a standard format used in information retrieval evaluation.
     * 
     * @param filePath Path to the TREC format file
     * @return List of Document objects
     * @throws IOException If an I/O error occurs
     */
    public List<Document> loadTrecDocuments(Path filePath) throws IOException {
        if (!Files.exists(filePath) || !Files.isRegularFile(filePath)) {
            throw new IOException("File does not exist or is not a regular file: " + filePath);
        }
        
        List<Document> documents = new ArrayList<>();
        StringBuilder currentDoc = new StringBuilder();
        String currentDocId = null;
        String currentTitle = null;
        boolean inDocument = false;
        
        try (BufferedReader reader = Files.newBufferedReader(filePath, StandardCharsets.UTF_8)) {
            String line;
            while ((line = reader.readLine()) != null) {
                if (line.startsWith("<DOC>")) {
                    inDocument = true;
                    currentDoc = new StringBuilder();
                } else if (line.startsWith("</DOC>")) {
                    inDocument = false;
                    if (currentDocId != null) {
                        // Create a new document and add it to the list
                        Document document = new Document(currentDocId, 
                                                        currentTitle != null ? currentTitle : currentDocId, 
                                                        currentDoc.toString());
                        documents.add(document);
                    }
                    currentDocId = null;
                    currentTitle = null;
                } else if (inDocument) {
                    if (line.startsWith("<DOCNO>") && line.endsWith("</DOCNO>")) {
                        currentDocId = line.substring(7, line.length() - 8).trim();
                    } else if (line.startsWith("<TITLE>") && line.endsWith("</TITLE>")) {
                        currentTitle = line.substring(7, line.length() - 8).trim();
                    } else if (!line.startsWith("<")) {
                        // Add content lines that are not XML tags
                        currentDoc.append(line).append("\n");
                    }
                }
            }
        }
        
        return documents;
    }
    
    /**
     * Creates a document from a string content.
     * Useful for testing or creating documents from in-memory content.
     * 
     * @param id Document ID
     * @param title Document title
     * @param content Document content
     * @return Document object
     */
    public Document createDocumentFromString(String id, String title, String content) {
        return new Document(id, title, content);
    }
    
    /**
     * Loads HTML documents from a directory.
     * This method extracts text content from HTML files.
     * 
     * @param directoryPath Path to the directory containing HTML files
     * @return List of Document objects
     * @throws IOException If an I/O error occurs
     */
    public List<Document> loadHtmlDocumentsFromDirectory(Path directoryPath) throws IOException {
        if (!Files.exists(directoryPath) || !Files.isDirectory(directoryPath)) {
            throw new IOException("Directory does not exist or is not a directory: " + directoryPath);
        }
        
        List<Path> htmlFiles = Files.walk(directoryPath)
                .filter(Files::isRegularFile)
                .filter(path -> {
                    String fileName = path.toString().toLowerCase();
                    return fileName.endsWith(".html") || fileName.endsWith(".htm");
                })
                .collect(Collectors.toList());
        
        List<Future<Document>> futures = new ArrayList<>();
        
        // Submit tasks to the executor service
        for (Path filePath : htmlFiles) {
            futures.add(executorService.submit(() -> {
                try {
                    String content = Files.readString(filePath, StandardCharsets.UTF_8);
                    // Simple HTML to text conversion (a more sophisticated parser would be better in production)
                    String textContent = content.replaceAll("<[^>]*>", "");
                    String fileName = filePath.getFileName().toString();
                    String id = fileName; // Use file name as ID for stability
                    
                    return new Document(id, fileName, textContent, filePath);
                } catch (IOException e) {
                    System.err.println("Error loading HTML document: " + filePath + ": " + e.getMessage());
                    return null;
                }
            }));
        }
        
        // Collect results
        List<Document> documents = new ArrayList<>();
        for (Future<Document> future : futures) {
            try {
                Document document = future.get();
                if (document != null) {
                    documents.add(document);
                }
            } catch (Exception e) {
                System.err.println("Error retrieving HTML document: " + e.getMessage());
            }
        }
        
        return documents;
    }
    
    /**
     * Shuts down the executor service.
     * Should be called when the document loader is no longer needed.
     */
    public void shutdown() {
        executorService.shutdown();
        try {
            if (!executorService.awaitTermination(10, TimeUnit.SECONDS)) {
                executorService.shutdownNow();
            }
        } catch (InterruptedException e) {
            executorService.shutdownNow();
            Thread.currentThread().interrupt();
        }
    }
    
    /**
     * Loads documents from a CSV file.
     * Each line in the CSV should represent a document with fields separated by commas.
     * 
     * @param filePath Path to the CSV file
     * @param idColumn Index of the column containing document IDs (0-based)
     * @param titleColumn Index of the column containing document titles (0-based)
     * @param contentColumn Index of the column containing document content (0-based)
     * @param hasHeader Whether the CSV file has a header row
     * @return List of Document objects
     * @throws IOException If an I/O error occurs
     */
    public List<Document> loadCsvDocuments(Path filePath, int idColumn, int titleColumn, int contentColumn, boolean hasHeader) throws IOException {
        if (!Files.exists(filePath) || !Files.isRegularFile(filePath)) {
            throw new IOException("File does not exist or is not a regular file: " + filePath);
        }
        
        List<Document> documents = new ArrayList<>();
        
        try (BufferedReader reader = Files.newBufferedReader(filePath, StandardCharsets.UTF_8)) {
            String line;
            boolean firstLine = true;
            
            while ((line = reader.readLine()) != null) {
                if (firstLine && hasHeader) {
                    firstLine = false;
                    continue;
                }
                
                String[] fields = line.split(",");
                if (fields.length > Math.max(Math.max(idColumn, titleColumn), contentColumn)) {
                    String id = fields[idColumn];
                    String title = fields[titleColumn];
                    String content = fields[contentColumn];
                    
                    Document document = new Document(id, title, content);
                    documents.add(document);
                }
                
                firstLine = false;
            }
        }
        
        return documents;
    }
}
