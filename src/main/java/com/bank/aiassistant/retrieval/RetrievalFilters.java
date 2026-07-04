package com.bank.aiassistant.retrieval;

import lombok.Getter;
import lombok.Setter;

/**
 * 在线检索额外过滤条件。
 *
 * 这些条件只会进一步缩小检索范围，不会绕过权限过滤。
 */
@Getter
@Setter
public class RetrievalFilters {

    private String documentType;

    private String department;

    private String versionNo;

    private String documentId;
}
