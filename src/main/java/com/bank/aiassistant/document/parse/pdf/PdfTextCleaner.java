package com.bank.aiassistant.document.parse.pdf;

import org.springframework.stereotype.Component;

import java.text.Normalizer;

/**
 * PDF 文本清洗组件。
 *
 * 负责去除页码、页眉页脚噪声、异常空白，并统一全角/半角字符。
 * 更复杂的银行模板规则后续可以继续在这里扩展。
 */
@Component
public class PdfTextCleaner {

    public String cleanPageText(String text) {
        if (text == null || text.isBlank()) {
            return "";
        }
        String normalized = Normalizer.normalize(text, Normalizer.Form.NFKC);
        String[] lines = normalized.split("\\R");
        StringBuilder builder = new StringBuilder();
        for (String line : lines) {
            String cleanedLine = cleanLine(line);
            if (!cleanedLine.isBlank()) {
                builder.append(cleanedLine).append("\n");
            }
        }
        return mergeBrokenLines(builder.toString()).trim();
    }

    private String cleanLine(String line) {
        String value = line == null ? "" : line.trim();
        value = value.replaceAll("\\s+", " ");
        value = value.replaceAll("^[-—]?\\s*\\d+\\s*[-—]?$", "");
        value = value.replaceAll("^第\\s*\\d+\\s*页\\s*(/\\s*共\\s*\\d+\\s*页)?$", "");
        value = value.replaceAll("^(内部资料|保密文件|银行内部资料)\\s*$", "");
        return value.trim();
    }

    /**
     * 合并异常换行：如果上一行不是明显句末标点，下一行又不是标题/条款开头，
     * 则认为这是 PDF 抽取导致的硬换行，合并为同一段。
     */
    private String mergeBrokenLines(String text) {
        String[] lines = text.split("\\R");
        StringBuilder builder = new StringBuilder();
        for (String line : lines) {
            if (builder.isEmpty()) {
                builder.append(line);
                continue;
            }
            String previous = builder.substring(Math.max(0, builder.length() - 1));
            if (previous.matches("[。！？；：.!?;:]") || looksLikeSectionStart(line)) {
                builder.append("\n").append(line);
            } else {
                builder.append(line.matches("^[a-zA-Z].*") ? " " : "").append(line);
            }
        }
        return builder.toString();
    }

    private boolean looksLikeSectionStart(String line) {
        return line.matches("^(第[一二三四五六七八九十百千万0-9]+[章节条].*|\\d+(\\.\\d+)*[、.\\s].*)");
    }
}
