package com.bank.aiassistant.document.parse;

import com.bank.aiassistant.document.message.DocumentParseMessage;
import com.bank.aiassistant.domain.entity.Document;
import com.bank.aiassistant.domain.enums.DocumentProcessStatus;
import com.bank.aiassistant.exception.BusinessException;
import com.bank.aiassistant.repository.DocumentRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
public class DocumentParseWorkflowService {

    private final DocumentRepository documentRepository;
    private final DocumentParser documentParser;

    /**
     * Executes the transactional parse workflow for a single message.
     *
     * Idempotency:
     * - If the document is already PARSING or PARSE_COMPLETED, the message is treated
     *   as a duplicate and skipped.
     * - A pessimistic write lock is taken before checking status so concurrent
     *   duplicate messages serialize around the same row.
     *
     * Transaction behavior:
     * - Status is changed to PARSING and flushed before the parser runs.
     * - If the parser throws, the transaction rolls back, so the document does not
     *   remain stuck in PARSING.
     * - On success, status is changed to PARSE_COMPLETED.
     */
    @Transactional(rollbackFor = Exception.class)
    public DocumentParseResult parse(DocumentParseMessage message) {
        Document document = documentRepository.findByIdForUpdate(message.documentId())
                .orElseThrow(() -> new BusinessException("DOCUMENT_NOT_FOUND", "Document not found"));

        if (document.getProcessStatus() == DocumentProcessStatus.PARSING
                || document.getProcessStatus() == DocumentProcessStatus.PARSE_COMPLETED) {
            log.info("Skip duplicate document parse message, documentId={}, status={}",
                    document.getDocumentId(), document.getProcessStatus());
            return DocumentParseResult.SKIPPED;
        }

        if (document.getProcessStatus() == DocumentProcessStatus.EXPIRED) {
            log.info("Skip expired document parse message, documentId={}", document.getDocumentId());
            return DocumentParseResult.SKIPPED;
        }

        // Mark as PARSING inside the same transaction that runs the parser.
        // If parser execution fails, this status update is rolled back.
        document.setProcessStatus(DocumentProcessStatus.PARSING);
        document.setRetryCount(message.retryCount());
        documentRepository.save(document);
        log.info("Document status changed to PARSING, documentId={}, retryCount={}",
                document.getDocumentId(), message.retryCount());

        // The real parser implementation is injected behind the DocumentParser
        // interface, so RabbitMQ retry logic stays independent of parsing details.
        documentParser.parse(document);

        document.setProcessStatus(DocumentProcessStatus.PARSE_COMPLETED);
        document.setParseErrorMessage(null);
        document.setRetryCount(message.retryCount());
        documentRepository.save(document);
        log.info("Document parse completed, documentId={}", document.getDocumentId());
        return DocumentParseResult.COMPLETED;
    }

    /**
     * Marks a document as permanently failed after retry exhaustion or DLQ handling.
     */
    @Transactional
    public void markFailed(String documentId, int retryCount, String errorMessage) {
        documentRepository.findById(documentId).ifPresent(document -> {
            document.setProcessStatus(DocumentProcessStatus.FAILED);
            document.setRetryCount(retryCount);
            document.setParseErrorMessage(truncate(errorMessage));
            documentRepository.save(document);
            log.info("Document status changed to FAILED, documentId={}, retryCount={}", documentId, retryCount);
        });
    }

    /**
     * Keeps parse error text within a database-friendly size.
     */
    private String truncate(String message) {
        if (message == null) {
            return null;
        }
        return message.length() <= 4000 ? message : message.substring(0, 4000);
    }
}
