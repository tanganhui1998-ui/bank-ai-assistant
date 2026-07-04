package com.bank.aiassistant.document.parse.pdf;

import com.bank.aiassistant.config.PdfParseProperties;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.sourceforge.tess4j.ITesseract;
import net.sourceforge.tess4j.Tesseract;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.rendering.ImageType;
import org.apache.pdfbox.rendering.PDFRenderer;
import org.springframework.stereotype.Service;

import java.awt.image.BufferedImage;

/**
 * 扫描版 PDF 的 OCR 兜底服务。
 *
 * OCR 依赖本机 tesseract/tessdata 环境，默认关闭。生产环境配置
 * app.ai.document.pdf.ocr-enabled=true 后，当某页没有可提取文本时会渲染图片并 OCR。
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class PdfOcrService {

    private final PdfParseProperties properties;

    public String recognizePage(PDDocument document, int zeroBasedPageIndex, String documentId) {
        if (!properties.isOcrEnabled()) {
            return "";
        }
        try {
            PDFRenderer renderer = new PDFRenderer(document);
            BufferedImage image = renderer.renderImageWithDPI(zeroBasedPageIndex, 200, ImageType.RGB);
            ITesseract tesseract = new Tesseract();
            tesseract.setLanguage(properties.getOcrLanguage());
            if (properties.getTessdataPath() != null && !properties.getTessdataPath().isBlank()) {
                tesseract.setDatapath(properties.getTessdataPath());
            }
            return tesseract.doOCR(image);
        } catch (Exception ex) {
            log.error("PDF OCR failed, documentId={}, pageNo={}", documentId, zeroBasedPageIndex + 1, ex);
            return "";
        }
    }
}
