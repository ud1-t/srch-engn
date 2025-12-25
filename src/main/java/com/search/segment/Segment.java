package com.search.segment;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.search.index.InvertedIndex;
import com.search.model.Document;
import com.search.model.Token;

public class Segment {
    private final int segmentId;
    private final Map<Integer, Document> documents = new HashMap<>();
    private InvertedIndex index;

    public Segment(int segmentId) {
        this.segmentId = segmentId;
        this.index = new InvertedIndex();
    }

    // Used during seeding/indexing
    public void addDocument(int docId, Document document, List<Token> tokens) {
        documents.put(docId, document);
        index.addDocument(docId, tokens);
    }

    // Used ONLY during cold start
    public void addDocument(int docId, Document document) {
        documents.put(docId, document);
    }

    public void setIndex(InvertedIndex index) { this.index = index; }

    public int getSegmentId() { return segmentId; }
    public Map<Integer, Document> getDocuments() { return Collections.unmodifiableMap(documents); }
    public InvertedIndex getIndex() { return index; }
}
