package com.bank.aiassistant.retrieval;

import com.bank.aiassistant.web.Result;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * AI 助手在线检索接口。
 *
 * 该接口面向 AI 对话编排层调用，返回切片内容、引用来源、融合分数和高亮片段。
 */
@Slf4j
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/ai/retrieval")
public class OnlineRetrievalController {

    private final OnlineRetrievalService onlineRetrievalService;
    private final RetrievalStatsService retrievalStatsService;

    @PostMapping
    public Result<RetrievalResponse> retrieve(@Valid @RequestBody RetrievalRequest request) {
        log.info("Received online retrieval request, question={}, topK={}", request.getQuestion(), request.getTopK());
        return Result.success(onlineRetrievalService.retrieve(request));
    }

    @GetMapping("/stats/high-frequency")
    public Result<List<RetrievalStatsItem>> highFrequencyQueries(@RequestParam(defaultValue = "20") int limit) {
        return Result.success(retrievalStatsService.highFrequencyQueries(limit));
    }

    @GetMapping("/stats/low-hit")
    public Result<List<RetrievalStatsItem>> lowHitQueries(@RequestParam(defaultValue = "20") int limit) {
        return Result.success(retrievalStatsService.lowHitQueries(limit));
    }
}
