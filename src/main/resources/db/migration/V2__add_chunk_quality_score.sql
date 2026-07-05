-- Add quality scoring for document chunks.

ALTER TABLE knowledge_chunk
    ADD COLUMN quality_score DOUBLE NULL AFTER token_count;

CREATE INDEX idx_chunk_quality_score ON knowledge_chunk (quality_score);
