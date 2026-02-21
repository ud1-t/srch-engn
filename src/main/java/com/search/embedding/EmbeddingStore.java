package com.search.embedding;

import com.google.gson.Gson;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.Map;

public class EmbeddingStore {

    private final Connection connection;
    private final Gson gson = new Gson();

    public EmbeddingStore(Connection connection) {
        this.connection = connection;
    }

    public void storeEmbedding(long canonicalDocId, float[] embedding) {
        String json = gson.toJson(embedding);
        try (PreparedStatement ps = connection.prepareStatement(
                "UPDATE canonical_documents SET embedding = ? WHERE id = ?")) {
            ps.setString(1, json);
            ps.setLong(2, canonicalDocId);
            ps.executeUpdate();
        } catch (SQLException e) {
            throw new RuntimeException("Failed to store embedding for doc " + canonicalDocId, e);
        }
    }

    public float[] getEmbedding(long canonicalDocId) {
        try (PreparedStatement ps = connection.prepareStatement(
                "SELECT embedding FROM canonical_documents WHERE id = ?")) {
            ps.setLong(1, canonicalDocId);
            ResultSet rs = ps.executeQuery();
            if (rs.next()) {
                String json = rs.getString("embedding");
                if (json == null) return null;
                return gson.fromJson(json, float[].class);
            }
            return null;
        } catch (SQLException e) {
            throw new RuntimeException("Failed to load embedding for doc " + canonicalDocId, e);
        }
    }

    public Map<Long, float[]> getAllEmbeddings() {
        Map<Long, float[]> result = new HashMap<>();
        try (PreparedStatement ps = connection.prepareStatement(
                "SELECT id, embedding FROM canonical_documents WHERE embedding IS NOT NULL")) {
            ResultSet rs = ps.executeQuery();
            while (rs.next()) {
                long id = rs.getLong("id");
                String json = rs.getString("embedding");
                result.put(id, gson.fromJson(json, float[].class));
            }
        } catch (SQLException e) {
            throw new RuntimeException("Failed to load all embeddings", e);
        }
        return result;
    }
}
