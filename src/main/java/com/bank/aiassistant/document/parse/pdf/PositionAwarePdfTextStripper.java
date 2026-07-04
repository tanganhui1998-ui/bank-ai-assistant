package com.bank.aiassistant.document.parse.pdf;

import com.bank.aiassistant.document.parse.model.TextLine;
import org.apache.pdfbox.text.PDFTextStripper;
import org.apache.pdfbox.text.TextPosition;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/**
 * 带位置信息的 PDF 文本提取器。
 *
 * PDFTextStripper 默认只能输出纯文本；这里重写 writeString，将 PDFBox 的
 * TextPosition 聚合成 TextLine，保留页码、坐标、字体大小和是否加粗。
 * 这些信息会用于标题识别、目录页判断、页眉页脚过滤。
 */
public class PositionAwarePdfTextStripper extends PDFTextStripper {

    private final List<TextLine> lines = new ArrayList<>();

    public PositionAwarePdfTextStripper() throws IOException {
        setSortByPosition(true);
    }

    public List<TextLine> getLines() {
        return lines.stream()
                .sorted(Comparator.comparingInt(TextLine::pageNo).thenComparing(TextLine::y))
                .toList();
    }

    @Override
    protected void writeString(String text, List<TextPosition> textPositions) {
        String normalized = text == null ? "" : text.trim();
        if (normalized.isBlank() || textPositions == null || textPositions.isEmpty()) {
            return;
        }

        float minX = Float.MAX_VALUE;
        float minY = Float.MAX_VALUE;
        float maxFontSize = 0F;
        boolean bold = false;

        for (TextPosition position : textPositions) {
            minX = Math.min(minX, position.getXDirAdj());
            minY = Math.min(minY, position.getYDirAdj());
            maxFontSize = Math.max(maxFontSize, position.getFontSizeInPt());
            String fontName = position.getFont() == null ? "" : position.getFont().getName();
            bold = bold || fontName.toLowerCase().contains("bold") || fontName.contains("黑体");
        }

        lines.add(TextLine.builder()
                .pageNo(getCurrentPageNo())
                .text(normalized)
                .x(minX)
                .y(minY)
                .fontSize(maxFontSize)
                .bold(bold)
                .build());
    }
}
