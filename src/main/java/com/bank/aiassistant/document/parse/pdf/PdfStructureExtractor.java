package com.bank.aiassistant.document.parse.pdf;

import com.bank.aiassistant.document.parse.model.ParsedPage;
import com.bank.aiassistant.document.parse.model.ParsedPdfDocument;
import com.bank.aiassistant.document.parse.model.ParsedTable;
import com.bank.aiassistant.document.parse.model.ParsedTitle;
import com.bank.aiassistant.document.parse.model.TextLine;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.springframework.stereotype.Component;
import technology.tabula.ObjectExtractor;
import technology.tabula.RectangularTextContainer;
import technology.tabula.Table;
import technology.tabula.extractors.SpreadsheetExtractionAlgorithm;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * PDF 结构化解析器。
 *
 * 这里聚合 PDFBox 文本位置信息和 Tabula 表格抽取结果，输出统一的
 * ParsedPdfDocument，供后续清洗和切片使用。
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class PdfStructureExtractor {

    private final PdfTextCleaner textCleaner;
    private final PdfOcrService ocrService;

    public ParsedPdfDocument extract(PDDocument document, String documentId) throws IOException {
        PositionAwarePdfTextStripper stripper = new PositionAwarePdfTextStripper();
        stripper.getText(document);

        Map<Integer, List<TextLine>> linesByPage = groupLinesByPage(stripper.getLines());
        List<ParsedPage> pages = new ArrayList<>();
        List<ParsedTitle> allTitles = new ArrayList<>();
        List<ParsedTable> allTables = extractTables(document, documentId);
        Map<Integer, List<ParsedTable>> tablesByPage = groupTablesByPage(allTables);

        for (int pageNo = 1; pageNo <= document.getNumberOfPages(); pageNo++) {
            List<TextLine> lines = linesByPage.getOrDefault(pageNo, List.of());
            String rawText = joinLines(lines);
            if (rawText.isBlank()) {
                rawText = ocrService.recognizePage(document, pageNo - 1, documentId);
            }

            boolean tocPage = isTableOfContentsPage(rawText);
            List<ParsedTitle> titles = detectTitles(lines, pageNo);
            allTitles.addAll(titles);

            pages.add(ParsedPage.builder()
                    .pageNo(pageNo)
                    .text(textCleaner.cleanPageText(rawText))
                    .tocPage(tocPage)
                    .titles(titles)
                    .tables(tablesByPage.getOrDefault(pageNo, List.of()))
                    .build());
        }

        return ParsedPdfDocument.builder()
                .pages(pages)
                .titles(allTitles)
                .tables(allTables)
                .build();
    }

    private Map<Integer, List<TextLine>> groupLinesByPage(List<TextLine> lines) {
        Map<Integer, List<TextLine>> result = new HashMap<>();
        for (TextLine line : lines) {
            result.computeIfAbsent(line.pageNo(), key -> new ArrayList<>()).add(line);
        }
        result.values().forEach(pageLines -> pageLines.sort(Comparator.comparing(TextLine::y).thenComparing(TextLine::x)));
        return result;
    }

    private String joinLines(List<TextLine> lines) {
        StringBuilder builder = new StringBuilder();
        for (TextLine line : lines) {
            // 通过位置规则过滤常见页眉页脚区域，减少重复噪声进入切片。
            if (line.y() < 40 || line.y() > 800) {
                continue;
            }
            builder.append(line.text()).append("\n");
        }
        return builder.toString();
    }

    /**
     * 目录页通常包含“目录”字样，并且多数行以页码结束。
     */
    private boolean isTableOfContentsPage(String text) {
        if (text == null || !text.contains("目录")) {
            return false;
        }
        String[] lines = text.split("\\R");
        int pageNumberLines = 0;
        int usefulLines = 0;
        for (String line : lines) {
            String value = line.trim();
            if (value.isBlank()) {
                continue;
            }
            usefulLines++;
            if (value.matches(".*(\\.{2,}|…{1,}|\\s)\\d{1,4}$")) {
                pageNumberLines++;
            }
        }
        return usefulLines > 3 && pageNumberLines * 2 >= usefulLines;
    }

    private List<ParsedTitle> detectTitles(List<TextLine> lines, int pageNo) {
        List<ParsedTitle> titles = new ArrayList<>();
        float averageFontSize = averageFontSize(lines);
        for (TextLine line : lines) {
            String text = line.text().trim();
            if (!looksLikeTitle(text, line, averageFontSize)) {
                continue;
            }
            String chapterNo = extractChapterNo(text);
            titles.add(ParsedTitle.builder()
                    .text(text)
                    .chapterNo(chapterNo)
                    .level(guessTitleLevel(text, line, averageFontSize))
                    .pageNo(pageNo)
                    .fontSize(line.fontSize())
                    .bold(line.bold())
                    .build());
        }
        return titles;
    }

    private float averageFontSize(List<TextLine> lines) {
        return (float) lines.stream()
                .map(TextLine::fontSize)
                .filter(size -> size > 0)
                .mapToDouble(Float::doubleValue)
                .average()
                .orElse(10D);
    }

    /**
     * 标题识别综合使用编号模式、字体大小和加粗信息。
     */
    private boolean looksLikeTitle(String text, TextLine line, float averageFontSize) {
        if (text.length() > 80) {
            return false;
        }
        return text.matches("^第[一二三四五六七八九十百千万0-9]+[章节条].*")
                || text.matches("^\\d+(\\.\\d+)*[、.\\s].{1,60}$")
                || ((line.bold() || line.fontSize() >= averageFontSize + 2) && text.length() <= 40);
    }

    private int guessTitleLevel(String text, TextLine line, float averageFontSize) {
        if (text.matches("^第[一二三四五六七八九十百千万0-9]+章.*")) {
            return 1;
        }
        if (text.matches("^\\d+\\s*[、.].*") || text.matches("^第[一二三四五六七八九十百千万0-9]+节.*")) {
            return 2;
        }
        if (text.matches("^\\d+\\.\\d+.*") || text.matches("^第[一二三四五六七八九十百千万0-9]+条.*")) {
            return 3;
        }
        return line.fontSize() >= averageFontSize + 4 ? 1 : 2;
    }

    private String extractChapterNo(String text) {
        if (text == null) {
            return null;
        }
        if (text.matches("^第[一二三四五六七八九十百千万0-9]+[章节条].*")) {
            int end = Math.min(firstWhitespaceOrEnd(text), Math.min(text.length(), 12));
            return text.substring(0, end);
        }
        if (text.matches("^\\d+(\\.\\d+)*[、.\\s].*")) {
            return text.replaceFirst("^(\\d+(\\.\\d+)*).*", "$1");
        }
        return null;
    }

    private int firstWhitespaceOrEnd(String text) {
        for (int i = 0; i < text.length(); i++) {
            if (Character.isWhitespace(text.charAt(i))) {
                return i;
            }
        }
        return text.length();
    }

    private List<ParsedTable> extractTables(PDDocument document, String documentId) {
        List<ParsedTable> tables = new ArrayList<>();
        try (ObjectExtractor extractor = new ObjectExtractor(document)) {
            SpreadsheetExtractionAlgorithm algorithm = new SpreadsheetExtractionAlgorithm();
            for (int pageNo = 1; pageNo <= document.getNumberOfPages(); pageNo++) {
                try {
                    technology.tabula.Page page = extractor.extract(pageNo);
                    List<Table> pageTables = algorithm.extract(page);
                    int tableIndex = 1;
                    for (Table table : pageTables) {
                        List<List<String>> rows = toRows(table);
                        if (!rows.isEmpty()) {
                            tables.add(ParsedTable.builder()
                                    .pageNo(pageNo)
                                    .title("第" + pageNo + "页表格" + tableIndex++)
                                    .rows(rows)
                                    .build());
                        }
                    }
                } catch (Exception ex) {
                    log.error("PDF table extraction failed, documentId={}, pageNo={}", documentId, pageNo, ex);
                }
            }
        } catch (Exception ex) {
            log.error("PDF table extractor initialization failed, documentId={}", documentId, ex);
        }
        return tables;
    }

    private List<List<String>> toRows(Table table) {
        List<List<String>> rows = new ArrayList<>();
        for (List<RectangularTextContainer> row : table.getRows()) {
            List<String> cells = new ArrayList<>();
            for (RectangularTextContainer cell : row) {
                cells.add(cell.getText().replaceAll("\\s+", " ").trim());
            }
            if (cells.stream().anyMatch(cell -> !cell.isBlank())) {
                rows.add(cells);
            }
        }
        return rows;
    }

    private Map<Integer, List<ParsedTable>> groupTablesByPage(List<ParsedTable> tables) {
        Map<Integer, List<ParsedTable>> result = new HashMap<>();
        for (ParsedTable table : tables) {
            result.computeIfAbsent(table.pageNo(), key -> new ArrayList<>()).add(table);
        }
        return result;
    }
}
