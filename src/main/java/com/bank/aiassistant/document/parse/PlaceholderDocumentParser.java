package com.bank.aiassistant.document.parse;

import com.bank.aiassistant.domain.entity.Document;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class PlaceholderDocumentParser implements DocumentParser {

    /**
     * Temporary implementation used until the real parser is introduced.
     * It deliberately does not modify database state; the workflow service owns all
     * status transitions around parser execution.
     */
    @Override
    public void parse(Document document) {
        log.info("Placeholder document parser invoked, documentId={}, objectKey={}",
                document.getDocumentId(), document.getOssObjectKey());
    }
}
