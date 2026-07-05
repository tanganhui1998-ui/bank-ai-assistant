package com.bank.aiassistant.domain.entity;

import com.bank.aiassistant.domain.enums.ChunkStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * 知识库切片对象。
 *
 * documentId 是数据库真实字段；document 仅用于解析后写入 ES 时携带文档元数据。
 */
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DocumentChunk {

    private String chunkId;
    private String documentId;
    private Document document;
    private String content;
    private String chapterPath;
    private String chapterNo;
    private Integer chunkSeq;
    private Integer startPage;
    private Integer endPage;
    private Integer tokenCount;
    private Double qualityScore;
    private ChunkStatus status;

    public Document getDocument() {
        if (document == null && documentId != null) {
            document = Document.builder().documentId(documentId).build();
        }
        return document;
    }

    public void setDocument(Document document) {
        this.document = document;
        this.documentId = document == null ? null : document.getDocumentId();
    }
}
