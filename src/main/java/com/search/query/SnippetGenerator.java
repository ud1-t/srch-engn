package com.search.query;

public class SnippetGenerator {
    
    public String generate(String content, int offset, String term) {
        int start = Math.max(0, offset - 30);
        int end = Math.min(content.length(), offset + term.length() + 30);

        while(content.charAt(start) != ' ' && start > 0) {
            start--;
        }
        while(content.charAt(end - 1) != ' ' && end < content.length()) {
            end++;
        }
        
        return content.substring(start, end);
    }
}
