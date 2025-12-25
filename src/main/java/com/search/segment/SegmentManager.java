package com.search.segment;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import com.search.index.InvertedIndex;
import com.search.index.Posting;
import com.search.model.Document;
import com.search.model.Token;

public class SegmentManager {

    private final List<Segment> segments = new ArrayList<>();

    public void addSegment(Segment segment) {
        segments.add(segment);
    }

    public List<Segment> getSegments() {
        return Collections.unmodifiableList(segments);
    }

    public void clear() {
        segments.clear();
    }

    public Segment getSegment(int segmentId) {
        return segments.stream()
            .filter(s -> s.getSegmentId() == segmentId)
            .findFirst()
            .orElseThrow(() ->
                new IllegalArgumentException("Segment not found: " + segmentId));
    }

    public Segment merge(int segAId, int segBId, int newSegmentId) {
        Segment A = getSegment(segAId);
        Segment B = getSegment(segBId);

        Segment merged = new Segment(newSegmentId);
        int nextDocId = 1;

        Set<String> seenUrls = new HashSet<>();

        for(Segment s : List.of(A, B)) {
            for(var entry : s.getDocuments().entrySet()) {
                Document canonicalDoc = entry.getValue();
                if(!seenUrls.add(canonicalDoc.getUrl())) {
                    continue;
                }

                List<Token> tokens = extractTokens(s.getIndex(), entry.getKey());
                merged.addDocument(nextDocId++, canonicalDoc, tokens);
            }
        }

        return merged;
    }

    private List<Token> extractTokens(InvertedIndex index, int docId) {
        List<Token> tokens = new ArrayList<>();

        index.getIndex().forEach((term, postings) -> {
            Posting posting = postings.get(docId);
            if (posting != null) {
                for (int i = 0; i < posting.getPositions().size(); i++) {
                    tokens.add(new Token(
                        term,
                        posting.getPositions().get(i),
                        posting.getOffsets().get(i)
                    ));
                }
            }
        });

        return tokens;
    }
}
