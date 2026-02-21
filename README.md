# srch-engn (Segmented Inverted Index + AI)

This repository contains a search engine built to understand how real-world search engines are designed beyond the basic "term → document list" idea. Extended with AI-powered hybrid search and RAG (Retrieval-Augmented Generation).

## What This Is

A single-node, multi-segment search engine that:
- Indexes Wikipedia pages

- Uses an inverted index with:
    - term frequency
    - positions
    - character offsets

- Supports:
    - boolean AND queries
    - TF-IDF ranking
    - **hybrid search** (TF-IDF + semantic vector search with RRF fusion)
    - **semantic search** (cosine similarity over Gemini embeddings)
    - **RAG** (retrieval-augmented generation for natural language Q&A)
    - snippet generation

- Persists data in PostgreSQL

- Uses immutable segments and explicit merges

The design deliberately mirrors how real search engines (Lucene-style) think about indexing.

## What This Models Correctly

This system accurately models several non-obvious realities of search engines.

### 1. Immutable Segments

- Each indexing operation creates a new segment

- A segment contains:
    - its own documents
    - its own inverted index

- Once created, a segment is never mutated

This makes indexing:
- failure-isolated
- concurrency-friendly
- conceptually simple

### 2. Canonical Documents vs Segment Documents

Documents are split into two identities:

**Canonical document**

- Unique URL
- Stored once
- Never deleted

**Segment document**

- Local doc_id
- Points to a canonical document
- Exists only inside a segment

This separates content identity from index identity.

### 3. Segment Visibility Instead of Deletion

- Segments are marked active = true | false
- Queries only run against active segments
- Inactive segments (and their postings) are retained


### 4. Segment Merging

- Two active segments can be merged:
  - A new segment is created
  - Documents are re-indexed
  - Old segments are marked inactive
  - No in-place mutation occurs

This exposes why:

- scoring changes after merges
- global statistics are expensive
- merges are operationally costly

### 5. Scoring Sensitivity (TF-IDF)

The system demonstrates that:

- The same document
- With the same query
- Can produce different scores depending on segment composition

This happens because:

- N (number of documents) changes
- DF is segment-scoped
- Merging alters statistical context

This is intentional and educational.

### 6. Hybrid Search (TF-IDF + Semantic)

Results from keyword search (TF-IDF) and semantic vector search (cosine similarity) are merged using Reciprocal Rank Fusion (RRF) with k=60. This demonstrates how modern search engines combine multiple ranking signals.

### 7. RAG (Retrieval-Augmented Generation)

Top-K documents from hybrid search are fed as context to an LLM (Gemini 2.5 Flash Lite) to generate direct, cited answers to natural language questions.

#

### This project explicitly avoids orthogonal problems that would hide the core lessons.

#

## High-Level Architecture

```
                    ┌────────────┐
                    │ Wikipedia  │
                    │  Fetcher   │
                    └─────┬──────┘
                          │
                    ┌─────▼──────┐
                    │    Text    │
                    │ Processing │
                    └─────┬──────┘
                          │
          ┌───────────────▼────────────────┐
          │       Segment (immutable)      │
          │               ~                │
          │   Documents + Inverted Index   │
          └───────────┬───────┬────────────┘
                      │       │
           ┌──────────▼──┐  ┌─▼──────────────┐
           │  PostgreSQL │  │ Gemini Embedding│
           │  Storage    │  │   (768-dim)     │
           │             │  └────────┬────────┘
           │ canonical   │           │
           │ _documents  │◄──────────┘
           │ (+ embedding│    stored as JSON
           │  TEXT col)  │
           │ segment_docs│
           │ postings    │
           │ terms       │
           │ segments    │
           └──────┬──────┘
                  │
       ┌──────────▼──────────┐
       │     Query Time      │
       │                     │
       │  ┌───────┐ ┌──────┐ │
       │  │TF-IDF │ │Cosine│ │
       │  │Keyword│ │Simil.│ │
       │  └───┬───┘ └──┬───┘ │
       │      └────┬────┘     │
       │      ┌────▼────┐     │
       │      │  RRF    │     │
       │      │ Fusion  │     │
       │      └────┬────┘     │
       │           │          │
       │     ┌─────▼─────┐   │
       │     │   RAG     │   │
       │     │ (optional)│   │
       │     │ Gemini LLM│   │
       │     └───────────┘   │
       └─────────────────────┘
```

## Repository Structure

```
fetch/
  WikipediaFetcher              Full article text via Wikipedia extract API

processing/
  Tokenization
  Normalization
  Stop-word filtering

index/
  InvertedIndex
  Posting
  Stores:
    - doc_id
    - term frequency
    - positions
    - offsets

segment/
  Segment
  SegmentManager
  Owns lifecycle and visibility of segments

storage/
  PostgreSQL persistence layer
  Explicit separation of:
    - documents
    - terms
    - postings
    - segments

embedding/                     ← NEW
  EmbeddingService             Gemini embedding API client (768-dim)
  EmbeddingStore               JSON text storage in PostgreSQL

query/
  Boolean query execution
  Snippet generation using offsets
  SemanticQueryEngine          ← NEW: cosine similarity search
  HybridQueryEngine            ← NEW: RRF fusion of TF-IDF + semantic

ranking/
  TF-IDF ranking implementation
  CosineSimilarity             ← NEW: vector similarity

rag/                           ← NEW
  LlmClient                   Gemini 2.5 Flash Lite API client
  ContextAssembler             Top-K doc context builder
  RagPipeline                  Full RAG orchestration

shell/
  Interactive CLI (SearchShell)
  Exposes:
    - seed
    - seed-file
    - search (hybrid)
    - semantic-search
    - ask (RAG)
    - reindex-embeddings
    - merge
```

## How to Run

### Prerequisites

- Java 17+
- Docker + Docker Compose
- Gemini API key (optional, for AI features)

### Environment Setup

```bash
# Set Gemini API key (required for semantic search, hybrid search, and RAG)
export GEMINI_API_KEY=your_key_here
```

Get a free API key from [Google AI Studio](https://aistudio.google.com/apikey).

Without the API key, the engine falls back to keyword-only TF-IDF search.

### Start PostgreSQL

```
docker-compose up -d
```

### Initialize Schema (if not using docker-compose init)

```
psql -h localhost -U search -d search -f schema.sql
```

If adding AI features to an existing database, run:
```sql
ALTER TABLE canonical_documents ADD COLUMN IF NOT EXISTS embedding TEXT;
```

### Build & Run

```
mvn clean package
java -jar target/search-engine.jar
```

## Supported Commands

- `seed <page-key>` — Fetch and index a Wikipedia page (also generates embedding if AI enabled)
- `seed-file <path>` — Seed multiple pages from a file
- `search <query>` — Hybrid search (TF-IDF + semantic) or keyword-only if AI disabled
- `semantic-search <query>` — Pure semantic vector search (requires GEMINI_API_KEY)
- `ask <question>` — RAG: retrieves docs, generates AI answer with citations (requires GEMINI_API_KEY)
- `reindex-embeddings` — Generate embeddings for all documents missing them (requires GEMINI_API_KEY)
- `merge <segA> <segB>` — Merge two segments
- `load` — Reload from database
- `exit` — Exit

## Why This Exists

This project exists to answer questions like:

- Why are search engines append-only?
- Why does scoring change after merges?
- Why is deletion expensive?
- Why do real systems tolerate approximation?
- How do hybrid search systems combine keyword and semantic signals?
- How does RAG ground LLM answers in retrieved evidence?

The answers emerge naturally once you try to build one.
