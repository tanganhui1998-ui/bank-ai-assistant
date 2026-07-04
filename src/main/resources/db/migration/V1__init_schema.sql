-- Initial schema for the bank AI assistant.
-- Keep this file immutable after it has been applied by Flyway.

CREATE TABLE IF NOT EXISTS ai_document (
    document_id VARCHAR(36) NOT NULL,
    file_name VARCHAR(255) NOT NULL,
    file_size BIGINT NOT NULL,
    file_type VARCHAR(64) NOT NULL,
    file_hash VARCHAR(64) NOT NULL,
    oss_bucket VARCHAR(128) NOT NULL,
    oss_object_key VARCHAR(512) NOT NULL,
    oss_etag VARCHAR(128) NULL,
    access_url VARCHAR(1024) NULL,
    display_name VARCHAR(255) NOT NULL,
    document_type VARCHAR(32) NOT NULL,
    version_no VARCHAR(64) NULL,
    department VARCHAR(128) NULL,
    effective_time DATETIME NULL,
    confidentiality_level VARCHAR(32) NOT NULL,
    applicable_scope VARCHAR(512) NULL,
    publishing_unit VARCHAR(128) NULL,
    process_status VARCHAR(32) NOT NULL,
    parse_error_message TEXT NULL,
    retry_count INT NOT NULL DEFAULT 0,
    uploader_id VARCHAR(64) NOT NULL,
    uploader_name VARCHAR(128) NOT NULL,
    created_time DATETIME NOT NULL,
    updated_time DATETIME NOT NULL,
    published_time DATETIME NULL,
    PRIMARY KEY (document_id),
    UNIQUE KEY uk_document_file_hash (file_hash),
    KEY idx_document_status (process_status),
    KEY idx_document_type (document_type),
    KEY idx_document_department (department),
    KEY idx_document_effective_time (effective_time),
    KEY idx_document_published_time (published_time),
    KEY idx_document_uploader (uploader_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='AI document metadata';

CREATE TABLE IF NOT EXISTS knowledge_chunk (
    chunk_id VARCHAR(64) NOT NULL,
    document_id VARCHAR(36) NOT NULL,
    content LONGTEXT NOT NULL,
    chapter_path VARCHAR(1024) NULL,
    chapter_no VARCHAR(128) NULL,
    chunk_seq INT NOT NULL,
    start_page INT NULL,
    end_page INT NULL,
    token_count INT NULL,
    status VARCHAR(32) NOT NULL,
    PRIMARY KEY (chunk_id),
    KEY idx_chunk_document_seq (document_id, chunk_seq),
    KEY idx_chunk_status (status),
    CONSTRAINT fk_chunk_document FOREIGN KEY (document_id) REFERENCES ai_document (document_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='Knowledge document chunks';

CREATE TABLE IF NOT EXISTS ai_conversation_message (
    message_id VARCHAR(36) NOT NULL,
    session_id VARCHAR(36) NOT NULL,
    parent_message_id VARCHAR(36) NULL,
    user_id VARCHAR(64) NOT NULL,
    user_question LONGTEXT NOT NULL,
    ai_answer LONGTEXT NULL,
    references_json JSON NULL,
    tool_calls_json JSON NULL,
    session_status VARCHAR(32) NOT NULL,
    created_time DATETIME NOT NULL,
    elapsed_ms BIGINT NULL,
    PRIMARY KEY (message_id),
    KEY idx_conversation_session (session_id),
    KEY idx_conversation_parent (parent_message_id),
    KEY idx_conversation_user_time (user_id, created_time),
    KEY idx_conversation_status (session_status)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='AI conversation messages';

CREATE TABLE IF NOT EXISTS ai_pending_operation (
    pending_id VARCHAR(36) NOT NULL,
    user_id VARCHAR(64) NOT NULL,
    operation_type VARCHAR(32) NOT NULL,
    tool_name VARCHAR(128) NULL,
    business_params_json JSON NOT NULL,
    operation_summary VARCHAR(1024) NOT NULL,
    status VARCHAR(32) NOT NULL,
    expire_time DATETIME NOT NULL,
    created_time DATETIME NOT NULL,
    confirmed_time DATETIME NULL,
    PRIMARY KEY (pending_id),
    KEY idx_pending_user_status (user_id, status),
    KEY idx_pending_expire_time (expire_time)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='AI pending write operations';

CREATE TABLE IF NOT EXISTS ai_tool_call_audit (
    call_id VARCHAR(36) NOT NULL,
    user_id VARCHAR(64) NOT NULL,
    tool_name VARCHAR(128) NOT NULL,
    input_params_json JSON NULL,
    call_result LONGTEXT NULL,
    permission_rejected TINYINT(1) NOT NULL DEFAULT 0,
    reject_reason VARCHAR(1024) NULL,
    called_time DATETIME NOT NULL,
    elapsed_ms BIGINT NULL,
    PRIMARY KEY (call_id),
    KEY idx_tool_call_user_time (user_id, called_time),
    KEY idx_tool_call_name (tool_name),
    KEY idx_tool_call_rejected (permission_rejected)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='AI tool call audit logs';

CREATE TABLE IF NOT EXISTS ai_retrieval_audit_log (
    log_id VARCHAR(36) NOT NULL,
    user_id VARCHAR(64) NOT NULL,
    user_name VARCHAR(128) NULL,
    question TEXT NOT NULL,
    question_hash VARCHAR(64) NOT NULL,
    hit_count INT NOT NULL,
    elapsed_ms BIGINT NOT NULL,
    document_ids VARCHAR(1024) NULL,
    max_score DOUBLE NULL,
    low_confidence TINYINT(1) NOT NULL,
    created_time DATETIME NOT NULL,
    PRIMARY KEY (log_id),
    KEY idx_retrieval_user_time (user_id, created_time),
    KEY idx_retrieval_question_time (question_hash, created_time),
    KEY idx_retrieval_low_confidence (low_confidence)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='AI retrieval audit logs';

CREATE TABLE IF NOT EXISTS ai_security_audit_log (
    audit_id VARCHAR(36) NOT NULL,
    user_id VARCHAR(64) NOT NULL,
    user_name VARCHAR(128) NULL,
    session_id VARCHAR(64) NULL,
    action_type VARCHAR(64) NOT NULL,
    tool_name VARCHAR(128) NULL,
    operation_type VARCHAR(64) NULL,
    input_params_json JSON NULL,
    result_json LONGTEXT NULL,
    rejected TINYINT(1) NOT NULL,
    reject_reason VARCHAR(1024) NULL,
    elapsed_ms BIGINT NULL,
    client_ip VARCHAR(64) NULL,
    created_time DATETIME NOT NULL,
    PRIMARY KEY (audit_id),
    KEY idx_security_audit_user_time (user_id, created_time),
    KEY idx_security_audit_session_time (session_id, created_time),
    KEY idx_security_audit_action (action_type),
    KEY idx_security_audit_rejected (rejected)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='AI security audit logs';

CREATE TABLE IF NOT EXISTS ai_business_order (
    order_id VARCHAR(36) NOT NULL,
    pending_id VARCHAR(36) NOT NULL,
    business_order_no VARCHAR(64) NOT NULL,
    user_id VARCHAR(64) NOT NULL,
    tool_name VARCHAR(128) NULL,
    status VARCHAR(32) NOT NULL,
    created_time DATETIME NOT NULL,
    PRIMARY KEY (order_id),
    UNIQUE KEY uk_business_order_pending (pending_id),
    KEY idx_business_order_user_time (user_id, created_time)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='AI confirmed business orders';
