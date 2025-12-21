package com.search.ranking;

import java.util.List;
import java.util.Map;

import com.search.index.InvertedIndex;

public interface Ranker {
    Map<Integer, Double> rank(List<String> terms, InvertedIndex index, int totalDocs);
}
