package com.bank.aiassistant.search;

import com.bank.aiassistant.web.Result;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * 知识库检索接口。
 */
@Slf4j
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/knowledge")
public class KnowledgeSearchController {

    private final KnowledgeSearchService knowledgeSearchService;

    @PostMapping("/search")
    public Result<List<KnowledgeSearchResult>> search(@Valid @RequestBody KnowledgeSearchRequest request) {
        log.info("Received knowledge search request, query={}, size={}", request.getQuery(), request.getSize());
        return Result.success(knowledgeSearchService.search(request));
    }
}
