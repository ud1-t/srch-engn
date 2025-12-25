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

    private static final String INSERT_CANONICAL = """
        INSERT INTO canonical_documents (url, title, content)
        VALUES (?, ?, ?)
        ON CONFLICT (url) DO NOTHING
        RETURNING id
    """;

    private static final String SELECT_CANONICAL_BY_URL =
        "SELECT id FROM canonical_documents WHERE url = ?";

    private static final String INSERT_SEGMENT_DOC = """
        INSERT INTO segment_documents
        (segment_id, doc_id, canonical_doc_id)
        VALUES (?, ?, ?)
    """;

    private static final String SELECT_CANONICAL_DOCS =
        "SELECT id, url, title, content FROM canonical_documents";

    private static final String SELECT_SEGMENT_DOCS =
        "SELECT doc_id, canonical_doc_id FROM segment_documents WHERE segment_id = ?";

    @Override
    public Map<String, Integer> saveCanonicalDocuments(List<Document> docs) {
        Map<String, Integer> result = new HashMap<>();

        try {
            connection.setAutoCommit(false);
            for (Document doc : docs) {
                try (PreparedStatement ps = connection.prepareStatement(INSERT_CANONICAL)) {
                    ps.setString(1, doc.getUrl());
                    ps.setString(2, doc.getTitle());
                    ps.setString(3, doc.getContent());

                    ResultSet rs = ps.executeQuery();
                    if (rs.next()) {
                        result.put(doc.getUrl(), rs.getInt(1));
                    } else {
                        try (PreparedStatement sel = connection.prepareStatement(SELECT_CANONICAL_BY_URL)) {
                            sel.setString(1, doc.getUrl());
                            ResultSet r = sel.executeQuery();
                            r.next();
                            result.put(doc.getUrl(), r.getInt(1));
                        }
                    }
                }
            }
            connection.commit();
            return result;
        } catch (SQLException e) {
            try { connection.rollback(); } catch (SQLException ignored) {}
            throw new RuntimeException(e);
        } finally {
            try { connection.setAutoCommit(true); } catch (SQLException ignored) {}
        }
    }

    @Override
    public Map<Integer, Integer> mapToSegment(int segmentId, List<Integer> canonicalDocIds) {
        Map<Integer, Integer> mapping = new HashMap<>();
        int docId = 1;

        try (PreparedStatement ps = connection.prepareStatement(INSERT_SEGMENT_DOC)) {
            for(int canonicalId : canonicalDocIds) {
                ps.setInt(1, segmentId);
                ps.setInt(2, docId);
                ps.setInt(3, canonicalId);
                ps.executeUpdate();
                mapping.put(canonicalId, docId++);
            }
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
        return mapping;
    }

    @Override
    public Map<Integer, Document> loadCanonicalDocuments() {
        Map<Integer, Document> docs = new HashMap<>();

        try (PreparedStatement ps = connection.prepareStatement(SELECT_CANONICAL_DOCS)) {
            ResultSet rs = ps.executeQuery();
            while (rs.next()) {
                docs.put(
                    rs.getInt("id"),
                    new Document(
                        rs.getString("url"),
                        rs.getString("title"),
                        rs.getString("content")
                    )
                );
            }
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
        return docs;
    }

    @Override
    public Map<Integer, Integer> loadSegmentDocuments(int segmentId) {
        Map<Integer, Integer> mapping = new HashMap<>();

        try (PreparedStatement ps = connection.prepareStatement(SELECT_SEGMENT_DOCS)) {

            ps.setInt(1, segmentId);
            ResultSet rs = ps.executeQuery();
            while (rs.next()) {
                mapping.put(
                    rs.getInt("doc_id"),
                    rs.getInt("canonical_doc_id")
                );
            }
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
        return mapping;
    }
}
