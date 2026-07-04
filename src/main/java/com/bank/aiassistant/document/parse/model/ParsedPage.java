package com.bank.aiassistant.document.parse.model;

import lombok.Builder;

import java.util.List;

/**
 * PDF 单页解析结果，保留页码、文本、目录页标记、标题和表格。
 */
@Builder
public record ParsedPage(
        int pageNo,
        String text,
        boolean tocPage,
        List<ParsedTitle> titles,
        List<ParsedTable> tables
) {
}
