/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package com.IR.SearchEngine.indexing;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.Optional;

import com.IR.SearchEngine.data.Posting;

/**
 * Represents the inverted index data structure.
 * This class stores the mapping from terms to their postings lists.
 * 
 * Responsibilities:
 * - Maintain the term-to-postings mapping efficiently
 * - Provide fast lookup operations for query processing
 * - Support index serialization and persistence
 * - Store collection statistics for retrieval models
 * 
 * Implementation notes:
 * - Should use efficient data structures for term dictionary
 * - Will leverage Lucene for advanced indexing features
 * - May implement skip lists or other optimizations for postings traversal
 * 
 * @author alexhere updated by feliks
 */

public class InvertedIndex {

    // Map yang menghubungkan kata (term) dengan daftar posting-nya
    private final Map<String, List<Posting>> index;

    public InvertedIndex() {
        this.index = new HashMap<>();
    }

    /**
     * menambahkan term ke indeks dengan ID dokumen dan posisi.
     * jika term sudah ada, tingkatkan frekuensi dan tambahkan posisi baru.
     *
     * @param term    Term yang akan ditambahkan
     * @param docId   ID dokumen tempat term ditemukan
     * @param position Posisi term dalam dokumen
     */
    public void addTerm(String term, int docId, int position) {

        // ambil atau buat daftar posting untuk term
        List<Posting> postings = index.computeIfAbsent(term, k -> new ArrayList<>());

        // cari apakah posting untuk dokumen ini sudah ada
        Optional<Posting> postingOpt = postings.stream()
                .filter(p -> p.getDocId() == docId)
                .findFirst();

        if (postingOpt.isPresent()) {
            // Jika ada, update frekuensi dan tambahkan posisi
            postingOpt.get().incrementFrequency();
            postingOpt.get().getPositions().add(position);
        } else {
            // jika tidak ada, buat posting baru
            // dengan frekuensi 1 dan posisi yang diberikan 
            postings.add(new Posting(docId, 1, new ArrayList<>(List.of(position))));
        }
    }

    /**
     * mendapatkan daftar posting untuk term tertentu.
     *
     * @param term term yang dicari
     * @return daftar posting untuk term, atau daftar kosong jika tidak ditemukan
     */
    public List<Posting> getPostings(String term) {
        return index.getOrDefault(term, Collections.emptyList());
    }

    /**
     * mendapatkan daftar semua term dalam indeks.
     *
     * @return set dari semua term yang ada dalam indeks
     */
    public Set<String> getVocabulary() {
        return index.keySet();
    }
}
