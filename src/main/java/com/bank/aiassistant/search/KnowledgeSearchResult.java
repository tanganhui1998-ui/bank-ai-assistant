package com.bank.aiassistant.search;

import lombok.Builder;

/**
 * 知识库检索结果。
 */
@Builder
public record KnowledgeSearchResult(
        String chunkId,
        String documentId,
        String documentName,
        String versionNo,
        String department,
        String confidentialityLevel,
        String chapterPath,
        String content,
        Double score
) {
}
