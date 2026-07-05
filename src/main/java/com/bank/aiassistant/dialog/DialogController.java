package com.bank.aiassistant.dialog;

import com.bank.aiassistant.web.Result;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.http.MediaType;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

/**
 * AI 助手对话入口。
 *
 * 所有用户消息先进入这里，由 DialogService 统一完成上下文加载、意图识别和路由分发。
 */
@Slf4j
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/assistant")
public class DialogController {

    private final DialogService dialogService;
    private final StreamingDialogService streamingDialogService;
    private final ConversationMemoryService conversationMemoryService;

    @PostMapping("/chat")
    public Result<ChatResponse> chat(@Valid @RequestBody ChatRequest request) {
        log.info("Received assistant chat request, sessionId={}", request.getSessionId());
        return Result.success(dialogService.chat(request));
    }

    @PostMapping(value = "/chat/stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public SseEmitter stream(@Valid @RequestBody StreamingChatRequest request) {
        log.info("Received assistant streaming chat request, conversationId={}", request.getConversationId());
        return streamingDialogService.stream(request);
    }

    @GetMapping("/conversations/{conversationId}")
    public Result<ConversationStatusResponse> conversationStatus(@PathVariable String conversationId) {
        ConversationSession session = conversationMemoryService.load(conversationId);
        return Result.success(ConversationStatusResponse.builder()
                .conversationId(conversationId)
                .state(session.state())
                .pendingIntent(session.pendingIntent())
                .slots(session.slots())
                .missingSlots(session.missingSlots())
                .pendingOperationId(session.pendingOperationId())
                .history(session.history())
                .build());
    }
}
