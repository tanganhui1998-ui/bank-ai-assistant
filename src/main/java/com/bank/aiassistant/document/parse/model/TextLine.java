package com.bank.aiassistant.document.parse.model;

import lombok.Builder;

/**
 * 从 PDFBox TextPosition 中聚合出来的一行文本，包含位置和字体信息，
 * 用于标题识别、页眉页脚过滤和目录页判断。
 */
@Builder
public record TextLine(
        int pageNo,
        String text,
        float x,
        float y,
        float fontSize,
        boolean bold
) {
}
