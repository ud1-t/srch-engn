package com.search.model;

public class Document {
    private final String url;
    private final String title;
    private final String content;

    public Document(String url, String title, String content) {
        this.url = url;
        this.title = title;
        this.content = content;
    }

    public String getUrl() { return url; }
    public String getTitle() { return title; }
    public String getContent() { return content; }
}
