package com.bank.aiassistant.document.dto;

import com.bank.aiassistant.domain.entity.DocumentChunk;
import com.bank.aiassistant.domain.enums.ChunkStatus;
import lombok.Builder;

/**
 * 管理端切片预览响应。
 */
@Builder
public record DocumentChunkPreviewResponse(
        String chunkId,
        String documentId,
        Integer chunkSeq,
        String chapterPath,
        String chapterNo,
        Integer startPage,
        Integer endPage,
        Integer tokenCount,
        Double qualityScore,
        ChunkStatus status,
        String contentPreview
) {

    public static DocumentChunkPreviewResponse from(DocumentChunk chunk) {
        String content = chunk.getContent() == null ? "" : chunk.getContent();
        return DocumentChunkPreviewResponse.builder()
                .chunkId(chunk.getChunkId())
                .documentId(chunk.getDocumentId())
                .chunkSeq(chunk.getChunkSeq())
                .chapterPath(chunk.getChapterPath())
                .chapterNo(chunk.getChapterNo())
                .startPage(chunk.getStartPage())
                .endPage(chunk.getEndPage())
                .tokenCount(chunk.getTokenCount())
                .qualityScore(chunk.getQualityScore())
                .status(chunk.getStatus())
                .contentPreview(content.length() <= 500 ? content : content.substring(0, 500))
                .build();
    }
}
