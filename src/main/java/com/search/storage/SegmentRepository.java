package com.search.storage;

import java.util.List;

public interface SegmentRepository {
    int createSegment();
    List<Integer> loadActiveSegmentIds();
    void deactivateSegment(int segmentId);
    boolean isCanonicalDocInActiveSegment(int canonicalDocId);
}
