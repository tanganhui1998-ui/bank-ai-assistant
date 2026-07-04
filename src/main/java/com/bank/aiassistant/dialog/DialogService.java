package com.bank.aiassistant.dialog;

import com.bank.aiassistant.config.DialogProperties;
import com.bank.aiassistant.context.CurrentUser;
import com.bank.aiassistant.context.CurrentUserProvider;
import com.bank.aiassistant.domain.entity.AiConversationMessage;
import com.bank.aiassistant.domain.enums.AiSessionStatus;
import com.bank.aiassistant.llm.QwenChatCompletionService;
import com.bank.aiassistant.llm.QwenChatMessage;
import com.bank.aiassistant.repository.AiConversationMessageRepository;
import com.bank.aiassistant.retrieval.CitationSource;
import com.bank.aiassistant.retrieval.RetrievalRequest;
import com.bank.aiassistant.retrieval.RetrievalResponse;
import com.bank.aiassistant.retrieval.RetrievalResultItem;
import com.bank.aiassistant.retrieval.OnlineRetrievalService;
import com.bank.aiassistant.security.AiSecuritySandboxService;
import com.bank.aiassistant.security.SecurityAuditEvent;
import com.bank.aiassistant.security.SecurityAuditService;
import com.bank.aiassistant.security.SecurityDecision;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * AI 助手对话入口编排服务。
 *
 * 这是用户请求进入系统后的第一站：加载会话上下文、检查 Redis 会话状态、
 * 执行意图识别、按意图路由，并保存会话历史。
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class DialogService {

    private final CurrentUserProvider currentUserProvider;
    private final ConversationMemoryService conversationMemoryService;
    private final IntentRecognitionService intentRecognitionService;
    private final OnlineRetrievalService onlineRetrievalService;
    private final QwenChatCompletionService chatCompletionService;
    private final FunctionCallingGateway functionCallingGateway;
    private final SseStreamGateway sseStreamGateway;
    private final AiConversationMessageRepository conversationRepository;
    private final DialogProperties dialogProperties;
    private final ObjectMapper objectMapper;
    private final AiSecuritySandboxService sandboxService;
    private final SecurityAuditService securityAuditService;

    @Transactional
    public ChatResponse chat(ChatRequest request) {
        long start = System.currentTimeMillis();
        CurrentUser user = currentUserProvider.currentUser();
        String sessionId = normalizeSessionId(request.getSessionId());
        String context = loadConversationContext(sessionId);
        ConversationSession session = conversationMemoryService.load(sessionId);

        ChatResponse response;
        if (session.state() == ConversationState.WAITING_SLOT) {
            response = handleSlotFilling(sessionId, request.getMessage(), session, start);
        } else if (session.state() == ConversationState.WAITING_CONFIRM) {
            response = handleConfirm(sessionId, request.getMessage(), session, start);
        } else {
            IntentRecognitionResult intent = intentRecognitionService.recognize(sessionId, request.getMessage(), context, user);
            SecurityDecision decision = sandboxService.checkIntent(user, intent);
            if (!decision.allowed()) {
                securityAuditService.record(SecurityAuditEvent.builder()
                        .user(user)
                        .sessionId(sessionId)
                        .actionType("INTENT_PERMISSION_DENIED")
                        .operationType(intent.intent().name())
                        .inputParams(Map.of("question", request.getMessage()))
                        .result(Map.of("message", decision.reason()))
                        .rejected(true)
                        .rejectReason(decision.reason())
                        .elapsedMs(System.currentTimeMillis() - start)
                        .build());
                response = buildResponse(sessionId, intent, decision.reason(), List.of(), start);
            } else {
                response = route(sessionId, request.getMessage(), context, intent, start);
            }
        }

        saveConversation(user, sessionId, request.getMessage(), response);
        sseStreamGateway.emit(sessionId, response.answer());
        return response;
    }

    private ChatResponse route(
            String sessionId,
            String message,
            String context,
            IntentRecognitionResult intent,
            long start
    ) {
        return switch (intent.intent()) {
            case POLICY_QA -> handlePolicyQa(sessionId, message, context, intent, start);
            case BUSINESS_QUERY -> handleBusinessQuery(sessionId, message, intent, start);
            case BUSINESS_EXECUTE -> handleBusinessExecute(sessionId, message, intent, start);
            case CHITCHAT -> handleChitchat(sessionId, message, context, intent, start);
            case AMBIGUOUS -> handleAmbiguous(sessionId, intent, start);
        };
    }

    private ChatResponse handlePolicyQa(
            String sessionId,
            String message,
            String context,
            IntentRecognitionResult intent,
            long start
    ) {
        RetrievalRequest retrievalRequest = new RetrievalRequest();
        retrievalRequest.setQuestion(message);
        retrievalRequest.setTopK(5);
        RetrievalResponse retrieval = onlineRetrievalService.retrieve(retrievalRequest);
        if (retrieval.lowConfidence()) {
            return buildResponse(sessionId, intent, retrieval.message(), List.of(), start);
        }

        String ragContext = buildRagContext(retrieval);
        String citations = String.join("\n", retrieval.citations());
        String prompt = dialogProperties.getRagAnswerPromptTemplate()
                .replace("{question}", message)
                .replace("{context}", context + "\n" + ragContext)
                .replace("{citations}", citations);
        String answer = chatCompletionService.chat(List.of(new QwenChatMessage("user", prompt)));
        return buildResponse(sessionId, intent, answer, retrieval.results().stream().map(RetrievalResultItem::citation).toList(), start);
    }

    private ChatResponse handleBusinessQuery(String sessionId, String message, IntentRecognitionResult intent, long start) {
        ToolRouteResult result = functionCallingGateway.query(message, intent.slots());
        return buildResponse(sessionId, intent, result.answer(), List.of(), start);
    }

    private ChatResponse handleBusinessExecute(String sessionId, String message, IntentRecognitionResult intent, long start) {
        ToolRouteResult result = functionCallingGateway.execute(message, intent.slots());
        if (result.waitingConfirm()) {
            conversationMemoryService.updateState(sessionId, ConversationState.WAITING_CONFIRM, intent.intent(), intent.slots());
        }
        return buildResponse(sessionId, intent, result.answer(), List.of(), start);
    }

    private ChatResponse handleChitchat(
            String sessionId,
            String message,
            String context,
            IntentRecognitionResult intent,
            long start
    ) {
        String prompt = dialogProperties.getChitchatPromptTemplate()
                .replace("{context}", context)
                .replace("{question}", message);
        String answer = chatCompletionService.chat(List.of(new QwenChatMessage("user", prompt)));
        return buildResponse(sessionId, intent, answer, List.of(), start);
    }

    private ChatResponse handleAmbiguous(String sessionId, IntentRecognitionResult intent, long start) {
        conversationMemoryService.updateState(sessionId, ConversationState.WAITING_SLOT, intent.intent(), intent.slots());
        String question = intent.clarifyQuestion() == null || intent.clarifyQuestion().isBlank()
                ? "请补充关键信息后我再继续处理。"
                : intent.clarifyQuestion();
        return buildResponse(sessionId, intent, question, List.of(), start);
    }

    private ChatResponse handleSlotFilling(String sessionId, String message, ConversationSession session, long start) {
        Map<String, Object> slots = new LinkedHashMap<>(session.slots() == null ? Map.of() : session.slots());
        slots.put("userSupplement", message);
        conversationMemoryService.updateState(sessionId, ConversationState.NORMAL, null, slots);
        IntentRecognitionResult intent = IntentRecognitionResult.builder()
                .intent(session.pendingIntent() == null ? IntentType.AMBIGUOUS : session.pendingIntent())
                .confidence(0.7D)
                .slots(slots)
                .routeTarget("SLOT_FILLED")
                .reason("等待补槽状态下直接合并用户补充信息")
                .build();
        return buildResponse(sessionId, intent, "已收到补充信息，我会继续处理该请求。", List.of(), start);
    }

    private ChatResponse handleConfirm(String sessionId, String message, ConversationSession session, long start) {
        boolean confirmed = message.matches(".*(确认|同意|是|提交|继续).*");
        boolean canceled = message.matches(".*(取消|否|不用|停止).*");
        conversationMemoryService.updateState(sessionId, ConversationState.NORMAL, null, Map.of());
        String answer = confirmed ? "已确认。待第十批接入后，这里会执行对应的待确认操作。"
                : canceled ? "已取消本次操作。"
                : "请回复“确认”或“取消”。";
        IntentRecognitionResult intent = IntentRecognitionResult.builder()
                .intent(IntentType.BUSINESS_EXECUTE)
                .confidence(0.8D)
                .slots(session.slots())
                .routeTarget("CONFIRM_HANDLER")
                .reason("等待确认状态下直接处理确认/取消")
                .build();
        return buildResponse(sessionId, intent, answer, List.of(), start);
    }

    private ChatResponse buildResponse(
            String sessionId,
            IntentRecognitionResult intent,
            String answer,
            List<CitationSource> citations,
            long start
    ) {
        return ChatResponse.builder()
                .sessionId(sessionId)
                .intent(intent.intent())
                .answer(answer)
                .citations(citations)
                .slots(intent.slots())
                .lowConfidence(intent.confidence() < dialogProperties.getLowConfidenceThreshold())
                .routeTarget(intent.routeTarget())
                .elapsedMs(System.currentTimeMillis() - start)
                .build();
    }

    private String buildRagContext(RetrievalResponse retrieval) {
        StringBuilder builder = new StringBuilder();
        int index = 1;
        for (RetrievalResultItem item : retrieval.results()) {
            builder.append("片段").append(index++).append("：")
                    .append(item.content()).append("\n引用：")
                    .append(item.citation().formatted()).append("\n\n");
        }
        return builder.toString();
    }

    private String loadConversationContext(String sessionId) {
        List<AiConversationMessage> messages = conversationRepository.findBySessionIdOrderByCreatedTimeAsc(sessionId);
        int from = Math.max(0, messages.size() - dialogProperties.getMaxContextMessages());
        StringBuilder builder = new StringBuilder();
        for (AiConversationMessage message : messages.subList(from, messages.size())) {
            builder.append("用户：").append(message.getUserQuestion()).append("\n");
            builder.append("助手：").append(message.getAiAnswer()).append("\n");
        }
        return builder.toString();
    }

    private void saveConversation(CurrentUser user, String sessionId, String question, ChatResponse response) {
        try {
            conversationRepository.save(AiConversationMessage.builder()
                    .sessionId(sessionId)
                    .userId(user.userId())
                    .userQuestion(question)
                    .aiAnswer(response.answer())
                    .referencesJson(objectMapper.writeValueAsString(response.citations()))
                    .toolCallsJson("{}")
                    .sessionStatus(AiSessionStatus.ACTIVE)
                    .createdTime(LocalDateTime.now())
                    .elapsedMs(response.elapsedMs())
                    .build());
        } catch (Exception ex) {
            log.warn("Failed to save conversation message, sessionId={}", sessionId, ex);
        }
    }

    private String normalizeSessionId(String sessionId) {
        return sessionId == null || sessionId.isBlank() ? UUID.randomUUID().toString() : sessionId;
    }
}
