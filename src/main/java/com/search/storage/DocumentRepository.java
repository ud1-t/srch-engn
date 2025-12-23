package com.search.storage;

import java.util.Map;

import com.search.model.Document;

public interface DocumentRepository {
    void saveDocuments(Map<Integer, Document> documents);
    Map<Integer, Document> loadDocuments();
}
