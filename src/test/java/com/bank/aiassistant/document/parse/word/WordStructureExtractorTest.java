package com.bank.aiassistant.document.parse.word;

import com.bank.aiassistant.document.parse.model.ParsedPdfDocument;
import org.apache.poi.xwpf.usermodel.XWPFDocument;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;

import static org.assertj.core.api.Assertions.assertThat;

class WordStructureExtractorTest {

    private final WordStructureExtractor extractor = new WordStructureExtractor();

    @Test
    void extractShouldReadDocxParagraphsAndDetectTitles() throws Exception {
        byte[] bytes;
        try (XWPFDocument document = new XWPFDocument();
             ByteArrayOutputStream outputStream = new ByteArrayOutputStream()) {
            document.createParagraph().createRun().setText("第一章 总则");
            document.createParagraph().createRun().setText("为规范外包人员入场管理，制定本办法。");
            document.write(outputStream);
            bytes = outputStream.toByteArray();
        }

        ParsedPdfDocument result = extractor.extract(new ByteArrayInputStream(bytes), "test.docx", "docx");

        assertThat(result.pages()).hasSize(1);
        assertThat(result.pages().get(0).text()).contains("第一章 总则", "外包人员入场管理");
        assertThat(result.titles()).extracting("text").contains("第一章 总则");
    }
}
