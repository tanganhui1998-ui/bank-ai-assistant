package com.bank.aiassistant.document.parse.word;

import com.bank.aiassistant.document.parse.model.ParsedPage;
import com.bank.aiassistant.document.parse.model.ParsedPdfDocument;
import com.bank.aiassistant.document.parse.model.ParsedTable;
import com.bank.aiassistant.document.parse.model.ParsedTitle;
import lombok.extern.slf4j.Slf4j;
import org.apache.poi.hwpf.HWPFDocument;
import org.apache.poi.hwpf.extractor.WordExtractor;
import org.apache.poi.xwpf.extractor.XWPFWordExtractor;
import org.apache.poi.xwpf.usermodel.XWPFDocument;
import org.apache.poi.xwpf.usermodel.XWPFParagraph;
import org.apache.poi.xwpf.usermodel.XWPFTable;
import org.apache.poi.xwpf.usermodel.XWPFTableCell;
import org.apache.poi.xwpf.usermodel.XWPFTableRow;
import org.springframework.stereotype.Component;

import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Word 文档结构抽取器。
 *
 * DOCX 使用 POI XWPF 提取段落、标题和表格；DOC 使用 HWPF 提取正文段落。
 * Word 没有稳定页码信息时统一记为第 1 页，后续可接入版式渲染获得真实页码。
 */
@Slf4j
@Component
public class WordStructureExtractor {

    private static final Pattern TITLE_PATTERN = Pattern.compile(
            "^(第[一二三四五六七八九十百千万]+[章节条款]|\\d+(\\.\\d+)*[、.．]?|[一二三四五六七八九十]+[、.．])\\s*.*");

    public ParsedPdfDocument extract(InputStream inputStream, String fileName, String fileType) {
        String normalizedType = fileType == null ? "" : fileType.toLowerCase(Locale.ROOT);
        String normalizedName = fileName == null ? "" : fileName.toLowerCase(Locale.ROOT);
        try {
            if ("docx".equals(normalizedType) || normalizedName.endsWith(".docx")) {
                return extractDocx(inputStream);
            }
            if ("doc".equals(normalizedType) || normalizedName.endsWith(".doc")) {
                return extractDoc(inputStream);
            }
            throw new IllegalArgumentException("Unsupported Word file type: " + fileType);
        } catch (Exception ex) {
            throw new IllegalStateException("Word document parse failed, fileName=" + fileName, ex);
        }
    }

    private ParsedPdfDocument extractDocx(InputStream inputStream) throws Exception {
        try (XWPFDocument document = new XWPFDocument(inputStream);
             XWPFWordExtractor extractor = new XWPFWordExtractor(document)) {
            List<ParsedTitle> titles = new ArrayList<>();
            StringBuilder text = new StringBuilder();
            for (XWPFParagraph paragraph : document.getParagraphs()) {
                String line = paragraph.getText() == null ? "" : paragraph.getText().trim();
                if (line.isBlank()) {
                    continue;
                }
                text.append(line).append("\n");
                detectTitle(line, paragraph.getStyle(), titles);
            }
            if (text.isEmpty()) {
                text.append(extractor.getText());
            }

            List<ParsedTable> tables = extractDocxTables(document);
            ParsedPage page = ParsedPage.builder()
                    .pageNo(1)
                    .text(text.toString())
                    .tocPage(false)
                    .titles(titles)
                    .tables(tables)
                    .build();
            log.info("DOCX structure extracted, paragraphChars={}, titleCount={}, tableCount={}",
                    text.length(), titles.size(), tables.size());
            return ParsedPdfDocument.builder()
                    .pages(List.of(page))
                    .titles(titles)
                    .tables(tables)
                    .build();
        }
    }

    private ParsedPdfDocument extractDoc(InputStream inputStream) throws Exception {
        try (HWPFDocument document = new HWPFDocument(inputStream);
             WordExtractor extractor = new WordExtractor(document)) {
            List<ParsedTitle> titles = new ArrayList<>();
            StringBuilder text = new StringBuilder();
            for (String paragraph : extractor.getParagraphText()) {
                String line = paragraph == null ? "" : paragraph.trim();
                if (line.isBlank()) {
                    continue;
                }
                text.append(line).append("\n");
                detectTitle(line, null, titles);
            }
            ParsedPage page = ParsedPage.builder()
                    .pageNo(1)
                    .text(text.toString())
                    .tocPage(false)
                    .titles(titles)
                    .tables(List.of())
                    .build();
            log.info("DOC structure extracted, paragraphChars={}, titleCount={}", text.length(), titles.size());
            return ParsedPdfDocument.builder()
                    .pages(List.of(page))
                    .titles(titles)
                    .tables(List.of())
                    .build();
        }
    }

    private void detectTitle(String line, String style, List<ParsedTitle> titles) {
        Matcher matcher = TITLE_PATTERN.matcher(line);
        boolean styleTitle = style != null && style.toLowerCase(Locale.ROOT).contains("heading");
        if (!matcher.matches() && !styleTitle) {
            return;
        }
        titles.add(ParsedTitle.builder()
                .text(line)
                .chapterNo(resolveChapterNo(line))
                .level(resolveLevel(line, style))
                .pageNo(1)
                .fontSize(0F)
                .bold(styleTitle)
                .build());
    }

    private String resolveChapterNo(String line) {
        Matcher matcher = TITLE_PATTERN.matcher(line);
        if (!matcher.matches()) {
            return null;
        }
        return matcher.group(1);
    }

    private int resolveLevel(String line, String style) {
        if (style != null && style.matches(".*[1-6].*")) {
            return Math.min(6, Character.digit(style.replaceAll("\\D+", "").charAt(0), 10));
        }
        if (line.startsWith("第") && (line.contains("章") || line.contains("节"))) {
            return line.contains("章") ? 1 : 2;
        }
        if (line.matches("^\\d+\\s*[、.．]?.*")) {
            return 1;
        }
        long dotCount = line.chars().filter(ch -> ch == '.').count();
        return (int) Math.min(6, dotCount + 1);
    }

    private List<ParsedTable> extractDocxTables(XWPFDocument document) {
        List<ParsedTable> tables = new ArrayList<>();
        int tableIndex = 1;
        for (XWPFTable table : document.getTables()) {
            List<List<String>> rows = new ArrayList<>();
            for (XWPFTableRow row : table.getRows()) {
                List<String> cells = new ArrayList<>();
                for (XWPFTableCell cell : row.getTableCells()) {
                    cells.add(cell.getText() == null ? "" : cell.getText().replaceAll("\\R+", " ").trim());
                }
                if (!cells.isEmpty()) {
                    rows.add(cells);
                }
            }
            if (!rows.isEmpty()) {
                tables.add(ParsedTable.builder()
                        .pageNo(1)
                        .title("Word表格" + tableIndex++)
                        .rows(rows)
                        .build());
            }
        }
        return tables;
    }
}
