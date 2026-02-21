package com.search.query;

import com.search.index.InvertedIndex;
import com.search.model.Document;
import com.search.ranking.Ranker;
import com.search.segment.Segment;
import com.search.segment.SegmentManager;
import com.search.storage.DocumentRepository;

import java.util.*;

public class HybridQueryEngine {

    private static final int RRF_K = 60;

    private final SegmentManager segmentManager;
    private final QueryParser queryParser;
    private final Ranker ranker;
    private final SemanticQueryEngine semanticQueryEngine;
    private final DocumentRepository documentRepository;

    public HybridQueryEngine(SegmentManager segmentManager,
                             QueryParser queryParser,
                             Ranker ranker,
                             SemanticQueryEngine semanticQueryEngine,
                             DocumentRepository documentRepository) {
        this.segmentManager = segmentManager;
        this.queryParser = queryParser;
        this.ranker = ranker;
        this.semanticQueryEngine = semanticQueryEngine;
        this.documentRepository = documentRepository;
    }

    public List<HybridResult> search(String query) {
        List<String> terms = queryParser.parse(query);

        // 1. TF-IDF keyword search across segments
        List<KeywordResult> keywordResults = keywordSearch(terms);

        // 2. Semantic search
        List<SemanticQueryEngine.ScoredDocument> semanticResults;
        try {
            semanticResults = semanticQueryEngine.search(query);
        } catch (Exception e) {
            System.err.println("Warning: Semantic search failed, using keyword-only results. " + e.getMessage());
            return keywordResults.stream()
                .map(kr -> new HybridResult(kr.canonicalDocId, kr.segment, kr.docId, kr.score, kr.title, kr.url))
                .toList();
        }

        // 3. RRF merge
        return mergeWithRRF(keywordResults, semanticResults, terms);
    }

    private List<KeywordResult> keywordSearch(List<String> terms) {
        List<KeywordResult> results = new ArrayList<>();
        Map<Integer, Document> canonicalDocs = documentRepository.loadCanonicalDocuments();

        for (Segment segment : segmentManager.getSegments()) {
            InvertedIndex index = segment.getIndex();
            QueryExecutor executor = new QueryExecutor(index);
            Set<Integer> matchingDocIds = executor.execute(terms);

            if (matchingDocIds.isEmpty()) continue;

            Map<Integer, Double> scores = ranker.rank(terms, index, segment.getDocuments().size());
            Map<Integer, Integer> segDocMapping = new HashMap<>();

            // Build reverse mapping: docId -> canonicalDocId
            for (var entry : segment.getDocuments().entrySet()) {
                Document doc = entry.getValue();
                for (var canonEntry : canonicalDocs.entrySet()) {
                    if (canonEntry.getValue().getUrl().equals(doc.getUrl())) {
                        segDocMapping.put(entry.getKey(), canonEntry.getKey());
                        break;
                    }
                }
            }

            for (int docId : matchingDocIds) {
                double score = scores.getOrDefault(docId, 0.0);
                Document doc = segment.getDocuments().get(docId);
                int canonicalId = segDocMapping.getOrDefault(docId, -1);
                results.add(new KeywordResult(canonicalId, segment, docId, score, doc.getTitle(), doc.getUrl()));
            }
        }

        results.sort(Comparator.comparingDouble(r -> -r.score));
        return results;
    }

    private List<HybridResult> mergeWithRRF(List<KeywordResult> keywordResults,
                                             List<SemanticQueryEngine.ScoredDocument> semanticResults,
                                             List<String> terms) {
        Map<Long, Double> rrfScores = new HashMap<>();
        Map<Long, KeywordResult> keywordMap = new HashMap<>();

        // Assign RRF scores from keyword ranking
        for (int rank = 0; rank < keywordResults.size(); rank++) {
            KeywordResult kr = keywordResults.get(rank);
            long cid = kr.canonicalDocId;
            rrfScores.merge(cid, 1.0 / (RRF_K + rank + 1), Double::sum);
            keywordMap.put(cid, kr);
        }

        // Assign RRF scores from semantic ranking
        Map<Long, Double> semanticScoreMap = new HashMap<>();
        for (int rank = 0; rank < semanticResults.size(); rank++) {
            var sr = semanticResults.get(rank);
            long cid = sr.canonicalDocId();
            rrfScores.merge(cid, 1.0 / (RRF_K + rank + 1), Double::sum);
            semanticScoreMap.put(cid, sr.score());
        }

        // Build hybrid results
        Map<Integer, Document> canonicalDocs = documentRepository.loadCanonicalDocuments();
        List<HybridResult> results = new ArrayList<>();

        for (var entry : rrfScores.entrySet()) {
            long cid = entry.getKey();
            double rrfScore = entry.getValue();

            KeywordResult kr = keywordMap.get(cid);
            if (kr != null) {
                results.add(new HybridResult(cid, kr.segment, kr.docId, rrfScore, kr.title, kr.url));
            } else {
                // Only found via semantic search — no segment match
                Document doc = canonicalDocs.get((int) cid);
                if (doc != null) {
                    results.add(new HybridResult(cid, null, -1, rrfScore, doc.getTitle(), doc.getUrl()));
                }
            }
        }

        results.sort(Comparator.comparingDouble(r -> -r.score));
        return results;
    }

    private static class KeywordResult {
        final long canonicalDocId;
        final Segment segment;
        final int docId;
        final double score;
        final String title;
        final String url;

        KeywordResult(long canonicalDocId, Segment segment, int docId, double score, String title, String url) {
            this.canonicalDocId = canonicalDocId;
            this.segment = segment;
            this.docId = docId;
            this.score = score;
            this.title = title;
            this.url = url;
        }
    }

    public record HybridResult(long canonicalDocId, Segment segment, int docId, double score, String title, String url) {}
}
