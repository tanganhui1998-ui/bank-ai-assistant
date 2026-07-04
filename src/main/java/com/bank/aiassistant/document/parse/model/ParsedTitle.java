package com.bank.aiassistant.document.parse.model;

import lombok.Builder;

/**
 * PDF 中识别出的标题节点。
 */
@Builder
public record ParsedTitle(
        String text,
        String chapterNo,
        int level,
        int pageNo,
        float fontSize,
        boolean bold
) {
}
