package com.bank.aiassistant.document.parse;

import org.springframework.stereotype.Service;

/**
 * 知识切片质量评估服务。
 *
 * 当前使用轻量规则打分，便于离线运行和单元测试；后续可以替换为模型打分或人工标注反馈。
 */
@Service
public class DocumentChunkQualityService {

    /**
     * 返回 0 到 1 之间的质量分。
     */
    public double score(String content) {
        if (content == null || content.isBlank()) {
            return 0D;
        }
        String text = content.trim();
        int length = text.length();
        double lengthScore = lengthScore(length);
        double alphaNumericRatio = alphaNumericRatio(text);
        double punctuationRatio = punctuationRatio(text);
        double duplicatePenalty = duplicateLinePenalty(text);

        double score = lengthScore;
        if (alphaNumericRatio < 0.25D) {
            score -= 0.2D;
        }
        if (punctuationRatio > 0.45D) {
            score -= 0.25D;
        }
        score -= duplicatePenalty;
        return Math.max(0D, Math.min(1D, score));
    }

    private double lengthScore(int length) {
        if (length < 40) {
            return 0.15D;
        }
        if (length < 120) {
            return 0.45D;
        }
        if (length < 1500) {
            return 0.95D;
        }
        return 0.8D;
    }

    private double alphaNumericRatio(String text) {
        long useful = text.chars()
                .filter(ch -> Character.isLetterOrDigit(ch) || isCjk(ch))
                .count();
        return useful * 1D / Math.max(text.length(), 1);
    }

    private boolean isCjk(int ch) {
        return ch >= 0x4E00 && ch <= 0x9FFF;
    }

    private double punctuationRatio(String text) {
        long punctuation = text.chars()
                .filter(ch -> !Character.isLetterOrDigit(ch) && !isCjk(ch) && !Character.isWhitespace(ch))
                .count();
        return punctuation * 1D / Math.max(text.length(), 1);
    }

    private double duplicateLinePenalty(String text) {
        String[] lines = text.split("\\R");
        if (lines.length < 4) {
            return 0D;
        }
        long distinct = java.util.Arrays.stream(lines)
                .map(String::trim)
                .filter(line -> !line.isBlank())
                .distinct()
                .count();
        double duplicateRatio = 1D - distinct * 1D / lines.length;
        return duplicateRatio > 0.5D ? 0.25D : 0D;
    }
}
