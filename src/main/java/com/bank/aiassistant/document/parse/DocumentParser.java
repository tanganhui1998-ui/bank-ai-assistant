package com.bank.aiassistant.document.parse;

import com.bank.aiassistant.domain.entity.Document;

/**
 * Extension point for the real parsing pipeline.
 *
 * The next implementation can download the document from OSS, extract text from PDF
 * or Word, split it into chunks, create embeddings and index the chunks into
 * Elasticsearch. Keeping this as an interface isolates RabbitMQ retry logic from
 * parsing implementation details.
 */
public interface DocumentParser {

    void parse(Document document);
}
