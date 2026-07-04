package com.bank.aiassistant.retrieval;

import lombok.Builder;

/**
 * 引用来源信息，用于回答末尾展示和审计溯源。
 */
@Builder
public record CitationSource(
        String documentName,
        String chapterPath,
        Integer pageNo,
        String documentType,
        String versionNo,
        String formatted
) {
}
