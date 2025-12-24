package com.search.storage;

import java.util.List;
import java.util.Map;

import com.search.model.Document;

public interface DocumentRepository {
    Map<String, Integer> saveDocuments(List<Document> documents);
    Map<Integer, Document> loadDocuments();
}
