package com.bank.aiassistant.retrieval;

import lombok.Builder;

/**
 * 单条检索结果。
 */
@Builder
public record RetrievalResultItem(
        String chunkId,
        String documentId,
        String content,
        String highlightedContent,
        Double score,
        CitationSource citation
) {
}
