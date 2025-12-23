package com.search;
// import java.sql.Connection;
// import java.sql.DriverManager;
// import java.sql.SQLException;
// import java.util.HashMap;
// import java.util.List;
// import java.util.Map;
// import java.util.Set;

// import com.search.fetch.DocumentFetcher;
// import com.search.fetch.impl.WikipediaFetcher;
// import com.search.index.InvertedIndex;
// import com.search.index.Posting;
// import com.search.model.Document;
// import com.search.processing.TextProcessor;
// import com.search.query.QueryExecutor;
// import com.search.query.QueryParser;
// import com.search.query.SnippetGenerator;
// import com.search.ranking.Ranker;
// import com.search.ranking.TfIdfRanker;
// import com.search.storage.DocumentRepository;
// import com.search.storage.IndexRepository;
// import com.search.storage.impl.DocumentRepositoryImpl;
// import com.search.storage.impl.IndexRepositoryImpl;
import com.search.shell.SearchShell;

public class Main {

    public static void main(String[] args) {

        new SearchShell().run();

        // Connection connection = DriverManager.getConnection(
        //         "jdbc:postgresql://localhost:5432/search",
        //         "search",
        //         "search"
        // );

        // IndexRepository indexRepository = new IndexRepositoryImpl(connection);
        // DocumentRepository documentRepository = new DocumentRepositoryImpl(connection);


        // // -----------------------------
        // // 1. FETCH DOCUMENTS
        // // -----------------------------
        // DocumentFetcher fetcher = new WikipediaFetcher();

        // Document doc1 = fetcher.fetch("https://en.wikipedia.org/wiki/Vikkstar123");
        // Document doc2 = fetcher.fetch("https://en.wikipedia.org/wiki/Wroetoshaw");
        // // Document doc3 = fetcher.fetch("https://en.wikipedia.org/wiki/KSI)");
        // Document doc4 = fetcher.fetch("https://en.wikipedia.org/wiki/Miniminter");
        // Document doc5 = fetcher.fetch("https://en.wikipedia.org/wiki/TBJZL");
        // Document doc6 = fetcher.fetch("https://en.wikipedia.org/wiki/Zerkaa");
        // Document doc7 = fetcher.fetch("https://en.wikipedia.org/wiki/Behzinga");

        // Map<Integer, Document> docStore = new HashMap<>();
        // docStore.put(1, doc1);
        // docStore.put(2, doc2);
        // // docStore.put(3, doc3);
        // docStore.put(4, doc4);
        // docStore.put(5, doc5);
        // docStore.put(6, doc6);
        // docStore.put(7, doc7);

        // // -----------------------------
        // // 2. PROCESS + INDEX
        // // -----------------------------
        // TextProcessor processor = new TextProcessor();

        // // use inverted index instead of indexbuilder for adding document
        // InvertedIndex index = new InvertedIndex();
        // index.addDocument(1, processor.process(doc1.getContent()));
        // index.addDocument(2, processor.process(doc2.getContent()));
        // // index.addDocument(3, processor.process(doc3.getContent()));
        // index.addDocument(4, processor.process(doc4.getContent()));
        // index.addDocument(5, processor.process(doc5.getContent()));
        // index.addDocument(6, processor.process(doc6.getContent()));
        // index.addDocument(7, processor.process(doc7.getContent()));

        // documentRepository.saveDocuments(docStore);
        // indexRepository.saveIndex(index);

        // // -----------------------------
        // // 3. PARSE QUERY
        // // -----------------------------
        // String query = "youtuber sidemen";

        // QueryParser queryParser = new QueryParser();
        // List<String> terms = queryParser.parse(query);

        // System.out.println("QUERY TERMS: " + terms);

        // // -----------------------------
        // // 4. BOOLEAN RETRIEVAL
        // // -----------------------------
        // QueryExecutor executor = new QueryExecutor(index);
        // Set<Integer> matchingDocs = executor.execute(terms);

        // System.out.println("MATCHING DOC IDS: " + matchingDocs);

        // // -----------------------------
        // // 5. TF-IDF RANKING (PHASE 5)
        // // -----------------------------
        // Ranker ranker = new TfIdfRanker();
        // Map<Integer, Double> scores =
        //         ranker.rank(terms, index, docStore.size());

        // System.out.println("\nRANKED RESULTS:");
        // scores.entrySet().stream()
        //         .sorted(Map.Entry.<Integer, Double>comparingByValue().reversed())
        //         .forEach(e -> {
        //             int docId = e.getKey();
        //             double score = e.getValue();
        //             printRankedResult(docId, score, terms, index, docStore);
        //         });

        // System.out.println();
        // System.out.println();
        // System.out.println("\nSAVING AND LOADING INDEX/DOC STORE FROM REPOSITORY...");
        // System.out.println();
        // System.out.println();
        
        // // load from repo, print everything again to validate
        // InvertedIndex loadedIndex = indexRepository.loadIndex();
        // Map<Integer, Document> loadedDocStore = documentRepository.loadDocuments();
        // Map<Integer, Double> loadedScores =
        //         ranker.rank(terms, loadedIndex, loadedDocStore.size());
        // System.out.println("\nRANKED RESULTS FROM LOADED INDEX/DOC STORE:");
        // loadedScores.entrySet().stream()
        //         .sorted(Map.Entry.<Integer, Double>comparingByValue().reversed())
        //         .forEach(e -> {
        //             int docId = e.getKey();
        //             double score = e.getValue();
        //             printRankedResult(docId, score, terms, loadedIndex, loadedDocStore);
        //         });
        
    }

    // -----------------------------
    // 6. SNIPPET GENERATION (PHASE 6)
    // -----------------------------
    // private static void printRankedResult(
    //     int docId,
    //     double score,
    //     List<String> terms,
    //     InvertedIndex index,
    //     Map<Integer, Document> docStore
    // ) {
    //     Document doc = docStore.get(docId);
    //     SnippetGenerator snippetGenerator = new SnippetGenerator();

    //     System.out.println("--------------------------------------------------");
    //     System.out.println("DOC ID: " + docId);
    //     System.out.println("TITLE: " + doc.getTitle());
    //     System.out.println("URL: " + doc.getUrl());
    //     System.out.println("SCORE: " + score);

    //     for (String term : terms) {
    //         Posting posting = index.getPostings(term).get(docId);
    //         if (posting != null && !posting.getOffsets().isEmpty()) {
    //             int offset = posting.getOffsets().get(0);
    //             String snippet =
    //                     snippetGenerator.generate(doc.getContent(), offset, term);
    //             System.out.println("SNIPPET [" + term + "]: " + snippet);
    //         }
    //     }
    // }
}