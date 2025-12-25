package com.search.model;

public class Document {
    private final String url;
    private final String title;
    private final String content;
    private final Integer segmentId;

    public Document(String url, String title, String content) {
        this.url = url;
        this.title = title;
        this.content = content;
        this.segmentId = null;
    }

    public Document(String url, String title, String content, int segmentId) {
        this.url = url;
        this.title = title;
        this.content = content;
        this.segmentId = segmentId;
    }

    public String getUrl() { return url; }
    public String getTitle() { return title; }
    public String getContent() { return content; }
    public int getSegmentId() {
        if(segmentId == null) {
            throw new IllegalStateException("Segment ID not set on transient Document");
        }
        return segmentId;
    }

    public boolean hasSegment() {
        return segmentId != null;
    }
}
