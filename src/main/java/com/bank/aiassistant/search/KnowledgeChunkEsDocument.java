package com.bank.aiassistant.search;

import lombok.Builder;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 写入 Elasticsearch 的知识库切片文档。
 *
 * 字段名称与索引 Mapping 保持一致，方便后续直接构造 DSL 查询。
 */
@Builder
public record KnowledgeChunkEsDocument(
        String chunkId,
        String documentId,
        String documentName,
        String documentType,
        String versionNo,
        String department,
        String confidentialityLevel,
        String status,
        String chunkStatus,
        Boolean latestVersion,
        String content,
        List<Float> embedding,
        String chapterPath,
        String chapterNo,
        Integer chunkSeq,
        Integer startPage,
        Integer endPage,
        Double qualityScore,
        LocalDateTime effectiveTime,
        LocalDateTime publishedTime,
        LocalDateTime createdTime
) {
}
