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

    private static final String SELECT_TERM_ID_SQL = """
        SELECT id FROM terms WHERE term = ?
    """;

    private static final String INSERT_TERM_SQL = """
        INSERT INTO terms (term, df)
        VALUES (?, 0)
        RETURNING id
    """;

    private static final String INCREMENT_DF_SQL = """
        UPDATE terms SET df = df + 1 WHERE id = ?
    """;
    private static final String INSERT_POSTING_SQL = """
        INSERT INTO postings (term_id, doc_id, tf, positions, offsets)
        VALUES (?, ?, ?, ?, ?)
        ON CONFLICT (term_id, doc_id) DO NOTHING
    """;
    private static final String SELECT_TERMS_SQL = """
        SELECT id, term FROM terms
        """;
    private static final String SELECT_POSTINGS_SQL = """
        SELECT term_id, doc_id, tf, positions, offsets
        FROM postings
        """;

    @Override
    public void appendDocument(int docId, List<Token> tokens) {
        try {
            connection.setAutoCommit(false);

            Map<String, List<Token>> byTerm = tokens.stream()
                    .collect(Collectors.groupingBy(Token::getTerm));

            for (var entry : byTerm.entrySet()) {
                String term = entry.getKey();
                List<Token> termTokens = entry.getValue();

                TermLookup lookup = getOrCreateTerm(term);
                int termId = lookup.id;
                incrementDf(termId);

                Posting posting = new Posting();
                for (Token t : termTokens) {
                    posting.addOccurrence(t.getPosition(), t.getOffset());
                }

                insertPosting(termId, docId, posting);
            }

            connection.commit();

        } catch (Exception e) {
            rollbackTransaction();
            throw new RuntimeException("Failed to append document " + docId, e);
        } finally {
            resetAutoCommit();
        }
    }

    private void rollbackTransaction() {
        try {
            connection.rollback();
        } catch (SQLException ignored) { }
    }

    @Override
    public InvertedIndex loadIndex() {
        try {
            Map<Integer, String> idTermMap = loadTerms();
            return loadPostings(idTermMap);
        } catch (SQLException e) {
            throw new RuntimeException("Error loading index", e);
        }
    }

    private TermLookup getOrCreateTerm(String term) throws SQLException {
        try (PreparedStatement ps = connection.prepareStatement(SELECT_TERM_ID_SQL)) {
            ps.setString(1, term);
            ResultSet rs = ps.executeQuery();
            if (rs.next()) {
                return new TermLookup(rs.getInt(1), false);
            }
        }

        try (PreparedStatement ps = connection.prepareStatement(INSERT_TERM_SQL)) {
            ps.setString(1, term);
            ResultSet rs = ps.executeQuery();
            rs.next();
            return new TermLookup(rs.getInt(1), true);
        }
    }

    private void incrementDf(int termId) throws SQLException {
        try (PreparedStatement ps = connection.prepareStatement(INCREMENT_DF_SQL)) {
            ps.setInt(1, termId);
            ps.executeUpdate();
        }
    }

    private void insertPosting(int termId, int docId, Posting posting) throws SQLException {
        try (PreparedStatement ps = connection.prepareStatement(INSERT_POSTING_SQL)) {
            ps.setInt(1, termId);
            ps.setInt(2, docId);
            ps.setInt(3, posting.getTermFrequency());
            ps.setArray(4, connection.createArrayOf("INTEGER", posting.getPositions().toArray()));
            ps.setArray(5, connection.createArrayOf("INTEGER", posting.getOffsets().toArray()));
            ps.executeUpdate();
        }
    }

    private void resetAutoCommit() {
        try {
            connection.setAutoCommit(true);
        } catch (SQLException ignored) { }
    }

    private Map<Integer, String> loadTerms() throws SQLException {
        Map<Integer, String> idTermMap = new HashMap<>();

        try (PreparedStatement ps = connection.prepareStatement(SELECT_TERMS_SQL)) {
            ResultSet rs = ps.executeQuery();
            while(rs.next()) {
                int id = rs.getInt("id");
                String term = rs.getString("term");
                idTermMap.put(id, term);
            }

        }

        return idTermMap;
    }

    private InvertedIndex loadPostings(Map<Integer, String> idTermMap) throws SQLException {
        InvertedIndex index = new InvertedIndex();

        try (PreparedStatement ps = connection.prepareStatement(SELECT_POSTINGS_SQL)) {
            ResultSet rs = ps.executeQuery();
            while(rs.next()) {
                int termId = rs.getInt("term_id");
                int docId = rs.getInt("doc_id");
                int tf = rs.getInt("tf");
                Integer[] positions = (Integer[]) rs.getArray("positions").getArray();
                Integer[] offsets = (Integer[]) rs.getArray("offsets").getArray();

                String term = idTermMap.get(termId);

                for(int i = 0; i < tf; i++) {
                    index.addTerm(term, docId, positions[i], offsets[i]);
                }
            }
        }
    
        return index;
    }

    private static class TermLookup {
        final int id;
        final boolean isNew;
        TermLookup(int id, boolean isNew) {
            this.id = id;
            this.isNew = isNew;
        }
    }

}
