package com.bank.aiassistant.document.parse.pdf;

import com.bank.aiassistant.config.PdfParseProperties;
import com.bank.aiassistant.document.parse.model.ChunkDraft;
import com.bank.aiassistant.document.parse.model.ParsedPage;
import com.bank.aiassistant.document.parse.model.ParsedPdfDocument;
import com.bank.aiassistant.document.parse.model.ParsedTable;
import com.bank.aiassistant.document.parse.model.ParsedTitle;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * PDF 智能切片器。
 *
 * 切片策略：
 * 1. 优先按标题边界切分，标题及其下属内容构成一个候选片段。
 * 2. 每个切片携带完整标题路径，便于检索加权和答案溯源。
 * 3. 目录页不生成文本切片。
 * 4. 超长内容按段落边界二次切分，并保留一定重叠文本。
 * 5. 表格独立生成切片，保留所属章节路径并转换为 Markdown。
 */
@Component
@RequiredArgsConstructor
public class PdfChunker {

    private final PdfParseProperties properties;

    public List<ChunkDraft> chunk(ParsedPdfDocument parsedDocument) {
        List<SectionBuffer> sections = buildTextSections(parsedDocument);
        List<ChunkDraft> chunks = new ArrayList<>();
        int seq = 1;

        for (SectionBuffer section : sections) {
            for (String part : splitWithOverlap(section.content())) {
                chunks.add(ChunkDraft.builder()
                        .content(part)
                        .chapterPath(section.chapterPath())
                        .chapterNo(section.chapterNo())
                        .chunkSeq(seq++)
                        .startPage(section.startPage())
                        .endPage(section.endPage())
                        .tokenCount(estimateTokenCount(part))
                        .build());
            }
        }

        for (ChunkDraft tableChunk : buildTableChunks(parsedDocument, seq)) {
            chunks.add(tableChunk);
            seq++;
        }
        return chunks;
    }

    private List<SectionBuffer> buildTextSections(ParsedPdfDocument parsedDocument) {
        List<SectionBuffer> sections = new ArrayList<>();
        SectionBuffer current = null;
        Map<Integer, ParsedTitle> titleByPageAndText = buildTitleLookup(parsedDocument);
        LinkedHashMap<Integer, String> titlePath = new LinkedHashMap<>();
        LinkedHashMap<Integer, String> chapterNoPath = new LinkedHashMap<>();

        for (ParsedPage page : parsedDocument.pages()) {
            if (page.tocPage()) {
                continue;
            }
            for (String line : page.text().split("\\R")) {
                String text = line.trim();
                if (text.isBlank()) {
                    continue;
                }

                ParsedTitle title = titleByPageAndText.get(titleKey(page.pageNo(), text));
                if (title != null) {
                    if (current != null && !current.isBlank()) {
                        sections.add(current);
                    }
                    updatePath(titlePath, chapterNoPath, title);
                    current = new SectionBuffer(buildPath(titlePath), title.chapterNo(), page.pageNo());
                    current.append(text);
                    continue;
                }

                if (current == null) {
                    current = new SectionBuffer(buildPath(titlePath), lastChapterNo(chapterNoPath), page.pageNo());
                }
                current.append(text);
                current.endPage(page.pageNo());
            }
        }

        if (current != null && !current.isBlank()) {
            sections.add(current);
        }
        return sections;
    }

    private Map<Integer, ParsedTitle> buildTitleLookup(ParsedPdfDocument parsedDocument) {
        Map<Integer, ParsedTitle> lookup = new HashMap<>();
        for (ParsedTitle title : parsedDocument.titles()) {
            lookup.put(titleKey(title.pageNo(), title.text()), title);
        }
        return lookup;
    }

    private int titleKey(int pageNo, String text) {
        return (pageNo + "::" + text.trim()).hashCode();
    }

    private void updatePath(
            LinkedHashMap<Integer, String> titlePath,
            LinkedHashMap<Integer, String> chapterNoPath,
            ParsedTitle title
    ) {
        titlePath.entrySet().removeIf(entry -> entry.getKey() >= title.level());
        chapterNoPath.entrySet().removeIf(entry -> entry.getKey() >= title.level());
        titlePath.put(title.level(), title.text());
        if (title.chapterNo() != null && !title.chapterNo().isBlank()) {
            chapterNoPath.put(title.level(), title.chapterNo());
        }
    }

    private String buildPath(LinkedHashMap<Integer, String> titlePath) {
        if (titlePath.isEmpty()) {
            return "正文";
        }
        return String.join(" → ", titlePath.values());
    }

    private String lastChapterNo(LinkedHashMap<Integer, String> chapterNoPath) {
        Optional<String> last = chapterNoPath.values().stream().reduce((left, right) -> right);
        return last.orElse(null);
    }

    private List<String> splitWithOverlap(String content) {
        String normalized = content == null ? "" : content.trim();
        if (normalized.isBlank()) {
            return List.of();
        }
        if (normalized.length() <= properties.getChunkMaxChars()) {
            return List.of(normalized);
        }

        List<String> parts = new ArrayList<>();
        int start = 0;
        while (start < normalized.length()) {
            int end = Math.min(start + properties.getChunkMaxChars(), normalized.length());
            if (end < normalized.length()) {
                end = findParagraphBoundary(normalized, start, end);
            }
            String part = normalized.substring(start, end).trim();
            if (!part.isBlank()) {
                parts.add(part);
            }
            if (end >= normalized.length()) {
                break;
            }
            start = Math.max(0, end - properties.getChunkOverlapChars());
        }
        return parts;
    }

    /**
     * 优先在段落或句号处切分，避免把一条制度条款切断。
     */
    private int findParagraphBoundary(String text, int start, int preferredEnd) {
        int minEnd = Math.min(start + properties.getChunkMinChars(), preferredEnd);
        int paragraph = text.lastIndexOf("\n", preferredEnd);
        if (paragraph >= minEnd) {
            return paragraph;
        }
        for (int i = preferredEnd - 1; i >= minEnd; i--) {
            char ch = text.charAt(i);
            if (ch == '。' || ch == '；' || ch == ';' || ch == '.') {
                return i + 1;
            }
        }
        return preferredEnd;
    }

    private List<ChunkDraft> buildTableChunks(ParsedPdfDocument parsedDocument, int startSeq) {
        List<ParsedTitle> sortedTitles = parsedDocument.titles().stream()
                .sorted(Comparator.comparingInt(ParsedTitle::pageNo))
                .toList();
        List<ChunkDraft> chunks = new ArrayList<>();
        int seq = startSeq;
        for (ParsedTable table : parsedDocument.tables()) {
            String markdown = table.toMarkdown();
            if (markdown.isBlank()) {
                continue;
            }
            String chapterPath = findNearestTitlePath(sortedTitles, table.pageNo());
            chunks.add(ChunkDraft.builder()
                    .content(markdown)
                    .chapterPath(chapterPath)
                    .chapterNo(null)
                    .chunkSeq(seq++)
                    .startPage(table.pageNo())
                    .endPage(table.pageNo())
                    .tokenCount(estimateTokenCount(markdown))
                    .build());
        }
        return chunks;
    }

    private String findNearestTitlePath(List<ParsedTitle> sortedTitles, int pageNo) {
        LinkedHashMap<Integer, String> path = new LinkedHashMap<>();
        for (ParsedTitle title : sortedTitles) {
            if (title.pageNo() > pageNo) {
                break;
            }
            path.entrySet().removeIf(entry -> entry.getKey() >= title.level());
            path.put(title.level(), title.text());
        }
        return buildPath(path);
    }

    private int estimateTokenCount(String text) {
        if (text == null || text.isBlank()) {
            return 0;
        }
        // 中文场景粗略估算：1 token 约等于 1.5 到 2 个汉字，这里取偏保守值。
        return Math.max(1, (int) Math.ceil(text.length() / 1.6D));
    }

    /**
     * 标题段落聚合缓冲区。
     */
    private static class SectionBuffer {
        private final String chapterPath;
        private final String chapterNo;
        private final int startPage;
        private int endPage;
        private final StringBuilder content = new StringBuilder();

        SectionBuffer(String chapterPath, String chapterNo, int startPage) {
            this.chapterPath = chapterPath;
            this.chapterNo = chapterNo;
            this.startPage = startPage;
            this.endPage = startPage;
        }

        void append(String text) {
            if (!content.isEmpty()) {
                content.append("\n");
            }
            content.append(text);
        }

        boolean isBlank() {
            return content.toString().isBlank();
        }

        String content() {
            return content.toString();
        }

        String chapterPath() {
            return chapterPath;
        }

        String chapterNo() {
            return chapterNo;
        }

        int startPage() {
            return startPage;
        }

        int endPage() {
            return endPage;
        }

        void endPage(int endPage) {
            this.endPage = endPage;
        }
    }
}
