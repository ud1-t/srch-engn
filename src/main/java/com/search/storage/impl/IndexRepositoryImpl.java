package com.search.storage.impl;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.HashMap;
import java.util.Map;

import com.search.index.InvertedIndex;
import com.search.index.Posting;
import com.search.storage.IndexRepository;

public class IndexRepositoryImpl implements IndexRepository {
    
    private final Connection connection;
    
    public IndexRepositoryImpl(Connection connection) {
        this.connection = connection;
    }

    private static final String DELETE_TERMS_SQL = """
        DELETE FROM terms
        """;
    private static final String DELETE_POSTINGS_SQL = """
        DELETE FROM postings
        """;
    private static final String INSERT_TERM_SQL = """
        INSERT INTO terms (term, df)
        VALUES (?, ?) returning id
        """;
    private static final String INSERT_POSTING_SQL = """
        INSERT INTO postings (term_id, doc_id, tf, positions, offsets)
        VALUES (?, ?, ?, ?, ?)
        """;
    private static final String SELECT_TERMS_SQL = """
        SELECT id, term FROM terms
        """;
    private static final String SELECT_POSTINGS_SQL = """
        SELECT term_id, doc_id, tf, positions, offsets
        FROM postings
        """;

    @Override
    public void saveIndex(InvertedIndex index) {
        try {
            connection.setAutoCommit(false);
            clearOldIndexSnapshot();
            Map<String, Integer> termIdMap = insertTerms(index);
            insertPostings(index, termIdMap);
            connection.commit();
        } catch (Exception e) {
            rollbackTransaction();
            throw new RuntimeException("Error saving index", e);
        } finally {
            resetAutoCommit();
        }
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

    private void clearOldIndexSnapshot() throws SQLException {
        try (Statement s = connection.createStatement()) {
            s.executeUpdate(DELETE_POSTINGS_SQL);
            s.executeUpdate(DELETE_TERMS_SQL);
        }
    }

    private Map<String, Integer> insertTerms(InvertedIndex index) throws SQLException {
        Map<String, Integer> termIdMap = new HashMap<>();
        try (PreparedStatement ps = connection.prepareStatement(INSERT_TERM_SQL)) {
            for(var termEntry: index.getIndex().entrySet()) {
                String term = termEntry.getKey();
                int df = termEntry.getValue().size();
            
                ps.setString(1, term);
                ps.setInt(2, df);

                var rs = ps.executeQuery();
                rs.next();

                int termId = rs.getInt(1);
                termIdMap.put(term, termId);
            }
        }

        return termIdMap;
    }

    private void insertPostings(InvertedIndex index, Map<String, Integer> termIdMap) throws SQLException {
        try (PreparedStatement ps = connection.prepareStatement(INSERT_POSTING_SQL)) {
            for(var termEntry: index.getIndex().entrySet()) {
                String term = termEntry.getKey();
                int termId = termIdMap.get(term);

                for(var postingEntry: termEntry.getValue().entrySet()) {
                    int docId = postingEntry.getKey();
                    Posting posting = postingEntry.getValue();

                    ps.setInt(1, termId);
                    ps.setInt(2, docId);
                    ps.setInt(3, posting.getTermFrequency());
                    ps.setArray(4,
                        connection.createArrayOf("INTEGER", posting.getPositions().toArray())
                    );
                    ps.setArray(5,
                        connection.createArrayOf("INTEGER", posting.getOffsets().toArray())
                    );

                    ps.addBatch();
                }
            }
            ps.executeBatch();
        }
    }

    private void rollbackTransaction() {
        try {
            connection.rollback();
        } catch (SQLException ignored) { }
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
}
