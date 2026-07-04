package com.bank.aiassistant.document.parse.model;

import lombok.Builder;

import java.util.List;

/**
 * PDF 全文解析结果。
 */
@Builder
public record ParsedPdfDocument(
        List<ParsedPage> pages,
        List<ParsedTitle> titles,
        List<ParsedTable> tables
) {
}
