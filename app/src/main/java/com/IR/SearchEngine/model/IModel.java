/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package com.IR.SearchEngine.model;

/**
 * Interface defining the contract for retrieval models.
 * All ranking models in the system should implement this interface.
 * 
 * Responsibilities:
 * - Define the standard methods for document scoring
 * - Ensure compatibility between different retrieval models
 * - Support model parameter configuration
 * - Provide methods for model evaluation and comparison
 * 
 * Implementation notes:
 * - Should be renamed to interface instead of class
 * - Will be implemented by concrete models like BM25 and VSM
 * 
 * @author alexhere
 */
public class IModel {
    
}
