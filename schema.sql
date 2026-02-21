CREATE TABLE IF NOT EXISTS segments (
    id SERIAL PRIMARY KEY,
    active BOOLEAN NOT NULL DEFAULT TRUE,
    created_at TIMESTAMP NOT NULL DEFAULT NOW()
);

CREATE TABLE IF NOT EXISTS canonical_documents (
    id SERIAL PRIMARY KEY,
    url TEXT UNIQUE NOT NULL,
    title TEXT NOT NULL,
    content TEXT NOT NULL,
    embedding TEXT
);

CREATE TABLE IF NOT EXISTS segment_documents (
    segment_id INTEGER NOT NULL REFERENCES segments(id),
    doc_id INTEGER NOT NULL,
    canonical_doc_id INTEGER NOT NULL REFERENCES canonical_documents(id),
    PRIMARY KEY (segment_id, doc_id)
);

CREATE TABLE IF NOT EXISTS terms (
    id SERIAL PRIMARY KEY,
    term TEXT UNIQUE NOT NULL,
    df INT NOT NULL
);

CREATE TABLE IF NOT EXISTS postings (
    segment_id INTEGER NOT NULL REFERENCES segments(id),
    term_id INTEGER NOT NULL REFERENCES terms(id),
    doc_id INTEGER NOT NULL,
    tf INT NOT NULL,
    positions INT[] NOT NULL,
    offsets INT[] NOT NULL,
    PRIMARY KEY (segment_id, term_id, doc_id)
);