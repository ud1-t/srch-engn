package com.search.storage.impl;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import com.search.index.InvertedIndex;
import com.search.index.Posting;
import com.search.model.Token;
import com.search.storage.IndexRepository;

public class IndexRepositoryImpl implements IndexRepository {

    private final Connection connection;

    public IndexRepositoryImpl(Connection connection) {
        this.connection = connection;
    }

    private static final String SELECT_TERM_ID_SQL =
        "SELECT id FROM terms WHERE term = ?";

    private static final String INSERT_TERM_SQL =
        "INSERT INTO terms (term, df) VALUES (?, 0) RETURNING id";

    private static final String INCREMENT_DF_SQL =
        "UPDATE terms SET df = df + 1 WHERE id = ?";

    private static final String INSERT_POSTING_SQL = """
        INSERT INTO postings
        (segment_id, term_id, doc_id, tf, positions, offsets)
        VALUES (?, ?, ?, ?, ?, ?)
    """;

    private static final String SELECT_POSTINGS_BY_SEGMENT = """
        SELECT term_id, doc_id, tf, positions, offsets
        FROM postings
        WHERE segment_id = ?
    """;

    @Override
    public void appendDocument(int segmentId, int docId, int canonicalDocId, List<Token> tokens) {
        try {
            connection.setAutoCommit(false);

            Map<String, List<Token>> byTerm = tokens.stream().collect(Collectors.groupingBy(Token::getTerm));

            for(var entry : byTerm.entrySet()) {
                String term = entry.getKey();
                List<Token> termTokens = entry.getValue();

                int termId = getOrCreateTerm(term);
                incrementDf(termId);

                Posting posting = new Posting();
                for(Token t : termTokens) {
                    posting.addOccurrence(t.getPosition(), t.getOffset());
                }

                try (PreparedStatement ps = connection.prepareStatement(INSERT_POSTING_SQL)) {
                    ps.setInt(1, segmentId);
                    ps.setInt(2, termId);
                    ps.setInt(3, docId);
                    ps.setInt(4, posting.getTermFrequency());
                    ps.setArray(5, connection.createArrayOf("INTEGER", posting.getPositions().toArray()));
                    ps.setArray(6, connection.createArrayOf("INTEGER", posting.getOffsets().toArray()));
                    ps.executeUpdate();
                }
            }

            connection.commit();
        } catch (Exception e) {
            rollback();
            throw new RuntimeException(e);
        } finally {
            resetAutoCommit();
        }
    }

    @Override
    public InvertedIndex loadIndexForSegment(int segmentId) {
        InvertedIndex index = new InvertedIndex();

        try (PreparedStatement ps = connection.prepareStatement(SELECT_POSTINGS_BY_SEGMENT)) {
            ps.setInt(1, segmentId);
            ResultSet rs = ps.executeQuery();
            Map<Integer, String> termMap = loadTerms();

            while (rs.next()) {
                String term = termMap.get(rs.getInt("term_id"));
                int docId = rs.getInt("doc_id");

                Integer[] positions = (Integer[]) rs.getArray("positions").getArray();
                Integer[] offsets = (Integer[]) rs.getArray("offsets").getArray();

                for(int i = 0; i < positions.length; i++) {
                    index.addTerm(term, docId, positions[i], offsets[i]);
                }
            }
            return index;
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }
    
    private int getOrCreateTerm(String term) throws SQLException {
        try (PreparedStatement ps = connection.prepareStatement(SELECT_TERM_ID_SQL)) {
            ps.setString(1, term);
            ResultSet rs = ps.executeQuery();
            if (rs.next()) return rs.getInt(1);
        }

        try (PreparedStatement ps = connection.prepareStatement(INSERT_TERM_SQL)) {
            ps.setString(1, term);
            ResultSet rs = ps.executeQuery();
            rs.next();
            return rs.getInt(1);
        }
    }

    private void incrementDf(int termId) throws SQLException {
        try (PreparedStatement ps = connection.prepareStatement(INCREMENT_DF_SQL)) {
            ps.setInt(1, termId);
            ps.executeUpdate();
        }
    }

    private Map<Integer, String> loadTerms() throws SQLException {
        Map<Integer, String> map = new HashMap<>();
        try (PreparedStatement ps = connection.prepareStatement("SELECT id, term FROM terms")) {
            ResultSet rs = ps.executeQuery();
            while (rs.next()) {
                map.put(rs.getInt("id"), rs.getString("term"));
            }
        }
        return map;
    }

    private void rollback() {
        try { connection.rollback(); } catch (SQLException ignored) {}
    }

    private void resetAutoCommit() {
        try { connection.setAutoCommit(true); } catch (SQLException ignored) {}
    }
}
