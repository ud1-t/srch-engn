package com.search.storage.impl;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

import com.search.storage.SegmentRepository;

public class SegmentRepositoryImpl implements SegmentRepository {

    private final Connection connection;

    public SegmentRepositoryImpl(Connection connection) {
        this.connection = connection;
    }

    private static final String INSERT_SEGMENT = """
        INSERT INTO segments DEFAULT VALUES
        RETURNING id
    """;
    private static final String SELECT_ACTIVE_SEGMENTS = """
        SELECT id FROM segments WHERE active = true ORDER BY id
    """;
    private static final String DEACTIVATE_SEGMENT =
        "UPDATE segments SET active = false WHERE id = ?";
    private static final String CHECK_ACTIVE_OWNERSHIP = """
        SELECT 1
        FROM segment_documents sd
        JOIN segments s ON sd.segment_id = s.id
        WHERE sd.canonical_doc_id = ?
        AND s.active = true
        LIMIT 1
    """;

    @Override
    public int createSegment() {
        try (PreparedStatement ps = connection.prepareStatement(INSERT_SEGMENT)) {
            ResultSet rs = ps.executeQuery();
            rs.next();
            return rs.getInt(1);

        } catch (SQLException e) {
            throw new RuntimeException("Failed to create segment", e);
        }
    }

    @Override
    public List<Integer> loadActiveSegmentIds() {
        try (PreparedStatement ps = connection.prepareStatement(SELECT_ACTIVE_SEGMENTS)) {
            ResultSet rs = ps.executeQuery();
            List<Integer> ids = new ArrayList<>();

            while (rs.next()) {
                ids.add(rs.getInt("id"));
            }
            return ids;

        } catch (SQLException e) {
            throw new RuntimeException("Failed to load segments", e);
        }
    }

    @Override
    public void deactivateSegment(int segmentId) {
        try (PreparedStatement ps = connection.prepareStatement(DEACTIVATE_SEGMENT)) {
            ps.setInt(1, segmentId);
            ps.executeUpdate();
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public boolean isCanonicalDocInActiveSegment(int canonicalDocId) {
        try (PreparedStatement ps = connection.prepareStatement(CHECK_ACTIVE_OWNERSHIP)) {
            ps.setInt(1, canonicalDocId);
            return ps.executeQuery().next();
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }
}
