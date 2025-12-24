package com.search.storage.impl;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.search.model.Document;
import com.search.storage.DocumentRepository;

public class DocumentRepositoryImpl implements DocumentRepository {
    
    private final Connection connection;

    public DocumentRepositoryImpl(Connection connection) {
        this.connection = connection;
    }

    private static final String INSERT_DOCUMENT_SQL = """
        INSERT INTO documents (url, title, content)
        VALUES (?, ?, ?)
        ON CONFLICT (url) DO NOTHING
        RETURNING id
        """;
    private static final String SELECT_DOCUMENT_SQL = """
        SELECT id, url, title, content FROM documents
        """;

    @Override
    public Map<String, Integer> saveDocuments(List<Document> docs) {
        Map<String, Integer> urlToId = new HashMap<>();

        try (PreparedStatement ps =
                connection.prepareStatement(INSERT_DOCUMENT_SQL)) {

            for (Document doc : docs) {
                ps.setString(1, doc.getUrl());
                ps.setString(2, doc.getTitle());
                ps.setString(3, doc.getContent());

                ResultSet rs = ps.executeQuery();
                if (rs.next()) {
                    int id = rs.getInt(1);
                    urlToId.put(doc.getUrl(), id);
                }
            }

            return urlToId;

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
