CREATE TABLE IF NOT EXISTS documents (
    id SERIAL PRIMARY KEY,
    url TEXT UNIQUE,
    title TEXT,
    content TEXT
);

CREATE TABLE IF NOT EXISTS terms (
    id SERIAL PRIMARY KEY,
    term TEXT UNIQUE,
    df INT
);

CREATE TABLE IF NOT EXISTS postings (
    term_id INT,
    doc_id INT,
    tf INT,
    positions INT[],
    offsets INT[],
    PRIMARY KEY (term_id, doc_id)
);
