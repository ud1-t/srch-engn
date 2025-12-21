package com.search.ranking;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.search.index.InvertedIndex;
import com.search.index.Posting;

public class TfIdfRanker implements Ranker {
    
    @Override
    public Map<Integer, Double> rank(List<String> terms, InvertedIndex index, int totalDocs) {
        Map<Integer, Double> scores = new HashMap<>();

        for(String term: terms) {
            Map<Integer, Posting> postings = index.getPostings(term);
            int df = postings.size();

            for(Map.Entry<Integer, Posting> entry: postings.entrySet()) {
                int docId = entry.getKey();
                int tf = entry.getValue().getTermFrequency();

                double idf = Math.log((totalDocs + 1.0) / (df + 1.0)) + 1.0;
                double score = tf * idf;
                scores.merge(docId, score, Double::sum);
            }
        }

        return scores;
    }
}
