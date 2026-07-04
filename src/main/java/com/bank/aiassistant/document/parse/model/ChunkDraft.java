package com.bank.aiassistant.document.parse.model;

import lombok.Builder;

/**
 * 切片器生成的中间结果，后续会转换为 DocumentChunk 实体入库。
 */
@Builder
public record ChunkDraft(
        String content,
        String chapterPath,
        String chapterNo,
        int chunkSeq,
        int startPage,
        int endPage,
        int tokenCount
) {
}
