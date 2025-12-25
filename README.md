# srch-engn (Segmented Inverted Index)

This repository contains a search engine built to understand how real-world search engines are designed beyond the basic “term → document list” idea.

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
          └───────────────┬────────────────┘
                          │
               ┌──────────▼───────────┐
               │ PostgreSQL Storage   │
               │                      │
               │ canonical_documents  │
               │ segment_documents    │
               │ postings             │
               │ terms                │
               │ segments             │
               └──────────────────────┘
```

At query time:

- Queries fan out across active segments
- Each segment produces scores
- Results are merged and ranked

## Repository Structure

```
fetch/
  WikipediaFetcher

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

query/
  Boolean query execution
  Snippet generation using offsets

ranking/
  TF-IDF ranking implementation

shell/
  Interactive CLI (SearchShell)
  Exposes:
    - seed
    - seed-file
    - search
    - merge
```

## How to Run

### Prerequisites

- Java 17+
- Docker + Docker Compose

Start PostgreSQL

```
docker-compose up -d
```

Initialize Schema

```
psql -h localhost -U search -d search -f schema.sql
```

Build & Run

```
mvn clean package
java -jar target/search-engine.jar
```

## Supported Commands

- `seed <page-key>`
- `seed-file <path>`
- `search <query>`
- `merge <segA> <segB>`
- `load`
- `exit`

## Why This Exists

This project exists to answer questions like:

- Why are search engines append-only?
- Why does scoring change after merges?
- Why is deletion expensive?
- Why do real systems tolerate approximation?

The answers emerge naturally once you try to build one.
