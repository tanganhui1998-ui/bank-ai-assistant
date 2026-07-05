package com.bank.aiassistant.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;

@Getter
@Setter
@ConfigurationProperties(prefix = "app.ai.document.pdf")
public class PdfParseProperties {

    /**
     * 是否启用扫描版 PDF 的 OCR 兜底能力。
     * 默认关闭，避免没有安装 tesseract/tessdata 时影响普通文本型 PDF 的解析。
     */
    private boolean ocrEnabled = false;

    /**
     * tessdata 目录路径。生产环境接入 OCR 时通过环境变量配置。
     */
    private String tessdataPath;

    /**
     * OCR 语言包，中文制度文档通常使用 chi_sim，也可配置为 chi_sim+eng。
     */
    private String ocrLanguage = "chi_sim";

    /**
     * 切片目标最小字符数。
     */
    private int chunkMinChars = 500;

    /**
     * 切片目标最大字符数。
     */
    private int chunkMaxChars = 1000;

    /**
     * 相邻切片重叠字符数，用于避免跨片上下文断裂。
     */
    private int chunkOverlapChars = 150;

    /**
     * 切片最低质量分，低于该值的切片会入库留痕，但不会写入 ES。
     */
    private double minQualityScore = 0.35D;
}
