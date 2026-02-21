package com.search.rag;

import com.search.model.Document;
import com.search.query.HybridQueryEngine;
import com.search.storage.DocumentRepository;

import java.util.List;
import java.util.Map;

public class RagPipeline {

    private static final int TOP_K = 5;
    private static final String SYSTEM_INSTRUCTION =
        "Answer the user's question using ONLY the provided documents. " +
        "Cite which document [Doc N] each fact comes from. " +
        "If the documents don't contain enough information, say so.";

    private final HybridQueryEngine hybridQueryEngine;
    private final ContextAssembler contextAssembler;
    private final LlmClient llmClient;
    private final DocumentRepository documentRepository;

    public RagPipeline(HybridQueryEngine hybridQueryEngine,
                       DocumentRepository documentRepository,
                       String apiKey) {
        this.hybridQueryEngine = hybridQueryEngine;
        this.documentRepository = documentRepository;
        this.contextAssembler = new ContextAssembler(documentRepository);
        this.llmClient = new LlmClient(apiKey);
    }

    public void ask(String question) {
        // 1. Hybrid search
        List<HybridQueryEngine.HybridResult> results = hybridQueryEngine.search(question);

        if (results.isEmpty()) {
            System.out.println("No relevant documents found for your question.");
            return;
        }

        // 2. Assemble context
        String context = contextAssembler.assemble(results, TOP_K);

        // 3. Build prompt
        String prompt = SYSTEM_INSTRUCTION + "\n\n" +
            "Documents:\n" + context + "\n" +
            "Question: " + question;

        // 4. Call LLM
        String answer = llmClient.generate(prompt);

        // 5. Display results
        System.out.println();
        System.out.println("AI Answer:");
        System.out.println(answer);
        System.out.println();

        // Show sources
        System.out.println("Sources:");
        Map<Integer, Document> canonicalDocs = documentRepository.loadCanonicalDocuments();
        int shown = 0;
        for (HybridQueryEngine.HybridResult r : results) {
            if (shown >= TOP_K) break;
            Document doc = canonicalDocs.get((int) r.canonicalDocId());
            if (doc == null) continue;
            shown++;
            System.out.printf("  [Doc %d] %s (score: %.4f)%n", shown, doc.getTitle(), r.score());
        }
        System.out.println();
    }
}
