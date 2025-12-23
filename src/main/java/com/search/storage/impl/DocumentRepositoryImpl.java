package com.search.storage.impl;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.Map;

import com.search.model.Document;
import com.search.storage.DocumentRepository;

public class DocumentRepositoryImpl implements DocumentRepository {
    
    private final Connection connection;

    public DocumentRepositoryImpl(Connection connection) {
        this.connection = connection;
    }

    private static final String INSERT_DOCUMENT_SQL = """
        INSERT INTO documents (id, url, title, content) VALUES (?, ?, ?, ?)
        ON CONFLICt (id) DO NOTHING
        """;
    private static final String SELECT_DOCUMENT_SQL = """
        SELECT id, url, title, content FROM documents
        """;

    @Override
    public void saveDocuments(Map<Integer, Document> documents) {
        try (PreparedStatement ps = connection.prepareStatement(INSERT_DOCUMENT_SQL)) {
            for(var entry: documents.entrySet()) {
                ps.setInt(1, entry.getKey());
                ps.setString(2, entry.getValue().getUrl());
                ps.setString(3, entry.getValue().getTitle());
                ps.setString(4, entry.getValue().getContent());
                ps.addBatch();
            }
            ps.executeBatch();
        } catch (SQLException e) {
            throw new RuntimeException("Error saving documents", e);
        }
    }

    @Override
    public Map<Integer, Document> loadDocuments() {
        // Implementation to load documents from the database
        try (PreparedStatement ps = connection.prepareStatement(SELECT_DOCUMENT_SQL)) {
            var rs = ps.executeQuery();
            Map<Integer, Document> documents = new HashMap<>();

            while(rs.next()) {
                int id = rs.getInt("id");
                String url = rs.getString("url");
                String title = rs.getString("title");
                String content = rs.getString("content");
                documents.put(id, new Document(url, title, content));
            }
            
            return documents;
        } catch (SQLException e) {
            throw new RuntimeException("Error loading documents", e);
        }
    }
}
