package com.search.query;

import com.search.embedding.EmbeddingService;
import com.search.embedding.EmbeddingStore;
import com.search.ranking.CosineSimilarity;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;

public class SemanticQueryEngine {

    private final EmbeddingService embeddingService;
    private final EmbeddingStore embeddingStore;

    public SemanticQueryEngine(EmbeddingService embeddingService, EmbeddingStore embeddingStore) {
        this.embeddingService = embeddingService;
        this.embeddingStore = embeddingStore;
    }

    public List<ScoredDocument> search(String query) {
        float[] queryEmbedding = embeddingService.embed(query);
        Map<Long, float[]> allEmbeddings = embeddingStore.getAllEmbeddings();

        List<ScoredDocument> results = new ArrayList<>();
        for (var entry : allEmbeddings.entrySet()) {
            double score = CosineSimilarity.compute(queryEmbedding, entry.getValue());
            results.add(new ScoredDocument(entry.getKey(), score));
        }

        results.sort(Comparator.comparingDouble(ScoredDocument::score).reversed());
        return results;
    }

    public record ScoredDocument(long canonicalDocId, double score) {}
}
