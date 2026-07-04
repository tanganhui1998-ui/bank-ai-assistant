package com.bank.aiassistant.document.parse.model;

import lombok.Builder;

import java.util.List;

/**
 * PDF 页面中的表格结构。
 */
@Builder
public record ParsedTable(
        int pageNo,
        String title,
        List<List<String>> rows
) {

    /**
     * 将表格转换为 Markdown，方便后续向量检索和答案溯源展示。
     */
    public String toMarkdown() {
        if (rows == null || rows.isEmpty()) {
            return "";
        }
        StringBuilder builder = new StringBuilder();
        if (title != null && !title.isBlank()) {
            builder.append("表格：").append(title).append("\n\n");
        }

        List<String> header = rows.get(0);
        builder.append("| ");
        builder.append(String.join(" | ", header));
        builder.append(" |\n| ");
        builder.append("--- | ".repeat(Math.max(header.size(), 1)));
        builder.append("\n");

        for (int i = 1; i < rows.size(); i++) {
            builder.append("| ");
            builder.append(String.join(" | ", rows.get(i)));
            builder.append(" |\n");
        }
        return builder.toString();
    }
}
