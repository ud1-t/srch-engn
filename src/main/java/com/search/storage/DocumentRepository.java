package com.search.storage;

import java.util.List;
import java.util.Map;

import com.search.model.Document;

public interface DocumentRepository {
    Map<String, Integer> saveCanonicalDocuments(List<Document> documents);
    Map<Integer, Document> loadCanonicalDocuments();
    Map<Integer, Integer> mapToSegment(int segmentId, List<Integer> canonicalDocIds);
    Map<Integer, Integer> loadSegmentDocuments(int segmentId);
}

