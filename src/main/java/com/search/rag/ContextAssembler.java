package com.search.rag;

import com.search.model.Document;
import com.search.query.HybridQueryEngine;
import com.search.storage.DocumentRepository;

import java.util.List;
import java.util.Map;

public class ContextAssembler {

    private static final int MAX_WORDS_PER_DOC = 3000;

    private final DocumentRepository documentRepository;

    public ContextAssembler(DocumentRepository documentRepository) {
        this.documentRepository = documentRepository;
    }

    public String assemble(List<HybridQueryEngine.HybridResult> results, int topK) {
        Map<Integer, Document> canonicalDocs = documentRepository.loadCanonicalDocuments();
        StringBuilder context = new StringBuilder();

        int count = 0;
        for (HybridQueryEngine.HybridResult r : results) {
            if (count >= topK) break;
            Document doc = canonicalDocs.get((int) r.canonicalDocId());
            if (doc == null) continue;

            count++;
            String truncatedContent = truncateToWords(doc.getContent(), MAX_WORDS_PER_DOC);
            context.append("[Doc ").append(count).append(": ").append(doc.getTitle()).append("] ");
            context.append(truncatedContent);
            context.append("\n\n");
        }

        return context.toString();
    }

    private static String truncateToWords(String text, int maxWords) {
        String[] words = text.split("\\s+");
        if (words.length <= maxWords) return text;

        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < maxWords; i++) {
            if (i > 0) sb.append(' ');
            sb.append(words[i]);
        }
        return sb.toString() + "...";
    }
}
