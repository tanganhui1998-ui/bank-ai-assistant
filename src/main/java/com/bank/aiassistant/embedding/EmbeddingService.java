package com.bank.aiassistant.embedding;

import java.util.List;

public interface EmbeddingService {

    /**
     * 生成单条文本向量。
     */
    List<Float> embed(String text);

    /**
     * 批量生成文本向量，返回顺序必须与输入顺序一致。
     */
    List<List<Float>> embedBatch(List<String> texts);
}
