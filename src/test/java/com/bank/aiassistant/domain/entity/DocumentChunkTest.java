package com.bank.aiassistant.domain.entity;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class DocumentChunkTest {

    @Test
    void setDocumentShouldSynchronizeDocumentIdForMyBatisPersistence() {
        Document document = Document.builder()
                .documentId("doc-001")
                .displayName("员工考勤管理办法")
                .build();
        DocumentChunk chunk = new DocumentChunk();

        chunk.setDocument(document);

        assertThat(chunk.getDocumentId()).isEqualTo("doc-001");
        assertThat(chunk.getDocument()).isSameAs(document);
    }

    @Test
    void getDocumentShouldCreateLightweightDocumentWhenOnlyDocumentIdExists() {
        DocumentChunk chunk = DocumentChunk.builder()
                .documentId("doc-002")
                .build();

        assertThat(chunk.getDocument().getDocumentId()).isEqualTo("doc-002");
    }
}
