package com.search.model;

public class Token {
    private final String term;
    private final int position;
    private final int offset;

    public Token(String term, int position, int offset) {
        this.term = term;
        this.position = position;
        this.offset = offset;
    }

    public String getTerm() { return term; }
    public int getPosition() { return position; }
    public int getOffset() { return offset; }
}
