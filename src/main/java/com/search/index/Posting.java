package com.search.index;

import java.util.ArrayList;
import java.util.List;

public class Posting {
    private int termFrequency;
    private final List<Integer> positions = new ArrayList<>();
    private final List<Integer> offsets = new ArrayList<>();

    public void addOccurrence(int position, int offset) {
        termFrequency++;
        positions.add(position);
        offsets.add(offset);
    }

    public int getTermFrequency() { return termFrequency; }
    public List<Integer> getPositions() { return positions; }
    public List<Integer> getOffsets() { return offsets; }
}
