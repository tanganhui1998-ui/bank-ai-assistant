package com.bank.aiassistant.dialog;

import com.bank.aiassistant.config.DialogProperties;
import com.bank.aiassistant.context.CurrentUser;
import com.bank.aiassistant.context.CurrentUserProvider;
import com.bank.aiassistant.llm.QwenChatMessage;
import com.bank.aiassistant.llm.QwenStreamingChatService;
import com.bank.aiassistant.retrieval.RetrievalRequest;
import com.bank.aiassistant.retrieval.RetrievalResponse;
import com.bank.aiassistant.retrieval.RetrievalResultItem;
import com.bank.aiassistant.retrieval.OnlineRetrievalService;
import com.bank.aiassistant.security.AiSecuritySandboxService;
import com.bank.aiassistant.security.SecurityAuditEvent;
import com.bank.aiassistant.security.SecurityAuditService;
import com.bank.aiassistant.security.SecurityDecision;
import com.bank.aiassistant.tool.ConfirmationExecutionResult;
import com.bank.aiassistant.tool.ConfirmationExecutionService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.Executor;
import java.util.concurrent.atomic.AtomicLong;

/**
 * SSE 流式对话编排服务。
 *
 * 在需要调用工具时先暂停模型输出，推送 tool_start/tool_result；
 * 如果工具返回待确认操作，则推送 confirm_required 并结束本轮流。
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class StreamingDialogService {

    private static final long SSE_TIMEOUT_MILLIS = 120_000L;

    private final CurrentUserProvider currentUserProvider;
    private final ConversationMemoryService memoryService;
    private final IntentRecognitionService intentRecognitionService;
    private final OnlineRetrievalService onlineRetrievalService;
    private final FunctionCallingGateway functionCallingGateway;
    private final ConfirmationExecutionService confirmationExecutionService;
    private final QwenStreamingChatService streamingChatService;
    private final DialogProperties dialogProperties;
    private final SseEventSender eventSender;
    private final AiSecuritySandboxService sandboxService;
    private final SecurityAuditService securityAuditService;

    @Qualifier("retrievalExecutor")
    private final Executor executor;

    public SseEmitter stream(StreamingChatRequest request) {
        SseEmitter emitter = new SseEmitter(SSE_TIMEOUT_MILLIS);
        String conversationId = normalizeConversationId(request.getConversationId());
        String messageId = normalizeMessageId(request.getClientMessageId());
        AtomicLong sequence = new AtomicLong();
        executor.execute(() -> handleStream(emitter, conversationId, messageId, request.getMessage(), sequence));
        emitter.onTimeout(() -> {
            log.warn("SSE connection timeout, conversationId={}", conversationId);
            eventSender.send(emitter, "error", conversationId, messageId, sequence, Map.of(
                    "code", "SSE_TIMEOUT",
                    "message", "连接超时，请重试。"
            ));
            emitter.complete();
        });
        emitter.onError(error -> log.warn("SSE connection error, conversationId={}", conversationId, error));
        return emitter;
    }

    private void handleStream(SseEmitter emitter, String conversationId, String messageId, String message, AtomicLong sequence) {
        CurrentUser user = currentUserProvider.currentUser();
        StringBuilder answerBuffer = new StringBuilder();
        try {
            eventSender.send(emitter, "conversation_start", conversationId, messageId, sequence, Map.of(
                    "conversationId", conversationId,
                    "messageId", messageId
            ));
            memoryService.appendMessage(conversationId, "user", message);
            ConversationSession session = memoryService.load(conversationId);
            if (session.state() == ConversationState.WAITING_CONFIRM) {
                handleWaitingConfirm(emitter, conversationId, messageId, message, session, answerBuffer, sequence);
                return;
            }

            String context = buildContext(session);
            IntentRecognitionResult intent = session.state() == ConversationState.COLLECTING_SLOTS || session.state() == ConversationState.WAITING_SLOT
                    ? buildSlotFillingIntent(session, message)
                    : intentRecognitionService.recognize(conversationId, message, context, user);
            SecurityDecision decision = sandboxService.checkIntent(user, intent);
            if (!decision.allowed()) {
                securityAuditService.record(SecurityAuditEvent.builder()
                        .user(user)
                        .sessionId(conversationId)
                        .actionType("INTENT_PERMISSION_DENIED")
                        .operationType(intent.intent().name())
                        .inputParams(Map.of("question", message))
                        .result(Map.of("message", decision.reason()))
                        .rejected(true)
                        .rejectReason(decision.reason())
                        .build());
                sendToken(emitter, conversationId, messageId, sequence, answerBuffer, decision.reason());
                sendDone(emitter, conversationId, messageId, sequence, answerBuffer, ConversationState.COMPLETED, null);
                emitter.complete();
                return;
            }

            eventSender.send(emitter, "status", conversationId, messageId, sequence, Map.of(
                    "stage", "intent_recognized",
                    "intent", intent.intent().name(),
                    "confidence", intent.confidence()
            ));

            switch (intent.intent()) {
                case POLICY_QA -> streamPolicyAnswer(emitter, conversationId, messageId, message, context, answerBuffer, sequence);
                case BUSINESS_QUERY -> streamBusinessQuery(emitter, conversationId, messageId, message, intent, answerBuffer, sequence);
                case BUSINESS_EXECUTE -> handleBusinessExecute(emitter, conversationId, messageId, message, intent, answerBuffer, sequence);
                case CHITCHAT -> streamChitchat(emitter, conversationId, messageId, message, context, answerBuffer, sequence);
                case AMBIGUOUS -> handleAmbiguous(emitter, conversationId, messageId, intent, answerBuffer, sequence);
            }
            memoryService.appendMessage(conversationId, "assistant", answerBuffer.toString());
            ConversationSession latest = memoryService.load(conversationId);
            sendDone(emitter, conversationId, messageId, sequence, answerBuffer, latest.state(), latest.pendingOperationId());
            emitter.complete();
        } catch (Exception ex) {
            log.error("Streaming dialog failed, conversationId={}, userId={}", conversationId, user.userId(), ex);
            eventSender.send(emitter, "error", conversationId, messageId, sequence, Map.of(
                    "code", "ASSISTANT_STREAM_FAILED",
                    "message", "对话处理失败，请稍后重试。"
            ));
            emitter.completeWithError(ex);
        }
    }

    private void streamPolicyAnswer(
            SseEmitter emitter,
            String conversationId,
            String messageId,
            String question,
            String context,
            StringBuilder answerBuffer,
            AtomicLong sequence
    ) {
        eventSender.send(emitter, "status", conversationId, messageId, sequence, Map.of("stage", "retrieving"));
        RetrievalRequest retrievalRequest = new RetrievalRequest();
        retrievalRequest.setQuestion(question);
        retrievalRequest.setTopK(5);
        RetrievalResponse retrieval = onlineRetrievalService.retrieve(retrievalRequest);
        if (retrieval.lowConfidence()) {
            eventSender.send(emitter, "low_confidence", conversationId, messageId, sequence, Map.of("message", retrieval.message()));
            sendToken(emitter, conversationId, messageId, sequence, answerBuffer, retrieval.message());
            return;
        }
        eventSender.send(emitter, "retrieval_result", conversationId, messageId, sequence, Map.of(
                "hitCount", retrieval.results().size(),
                "citations", retrieval.citations(),
                "trace", retrieval.trace()
        ));
        String prompt = dialogProperties.getRagAnswerPromptTemplate()
                .replace("{question}", question)
                .replace("{context}", context + "\n" + buildRagContext(retrieval))
                .replace("{citations}", String.join("\n", retrieval.citations()));
        streamingChatService.stream(List.of(new QwenChatMessage("user", prompt)),
                token -> sendToken(emitter, conversationId, messageId, sequence, answerBuffer, token));
    }

    private void streamBusinessQuery(
            SseEmitter emitter,
            String conversationId,
            String messageId,
            String message,
            IntentRecognitionResult intent,
            StringBuilder answerBuffer,
            AtomicLong sequence
    ) {
        eventSender.send(emitter, "tool_start", conversationId, messageId, sequence, Map.of("intent", intent.intent().name()));
        ToolRouteResult result = functionCallingGateway.query(message, intent.slots());
        eventSender.send(emitter, "tool_result", conversationId, messageId, sequence, result.data());
        sendToken(emitter, conversationId, messageId, sequence, answerBuffer, result.answer());
    }

    private void handleBusinessExecute(
            SseEmitter emitter,
            String conversationId,
            String messageId,
            String message,
            IntentRecognitionResult intent,
            StringBuilder answerBuffer,
            AtomicLong sequence
    ) {
        eventSender.send(emitter, "tool_start", conversationId, messageId, sequence, Map.of("intent", intent.intent().name()));
        ToolRouteResult result = functionCallingGateway.execute(message, intent.slots());
        eventSender.send(emitter, "tool_result", conversationId, messageId, sequence, result.data());
        if (result.waitingConfirm()) {
            memoryService.updateState(conversationId, ConversationState.WAITING_CONFIRM, intent.intent(), intent.slots(), List.of(), result.pendingOperationId());
            eventSender.send(emitter, "confirm_required", conversationId, messageId, sequence, Map.of(
                    "pendingOperationId", result.pendingOperationId(),
                    "summary", result.answer(),
                    "actions", List.of("confirm", "cancel"),
                    "expireSeconds", 300
            ));
        }
        sendToken(emitter, conversationId, messageId, sequence, answerBuffer, result.answer());
    }

    private void streamChitchat(
            SseEmitter emitter,
            String conversationId,
            String messageId,
            String question,
            String context,
            StringBuilder answerBuffer,
            AtomicLong sequence
    ) {
        String prompt = dialogProperties.getChitchatPromptTemplate()
                .replace("{context}", context)
                .replace("{question}", question);
        streamingChatService.stream(List.of(new QwenChatMessage("user", prompt)),
                token -> sendToken(emitter, conversationId, messageId, sequence, answerBuffer, token));
    }

    private void handleAmbiguous(
            SseEmitter emitter,
            String conversationId,
            String messageId,
            IntentRecognitionResult intent,
            StringBuilder answerBuffer,
            AtomicLong sequence
    ) {
        memoryService.updateState(conversationId, ConversationState.COLLECTING_SLOTS, intent.intent(), intent.slots(), intent.missingSlots(), null);
        eventSender.send(emitter, "slot_required", conversationId, messageId, sequence, Map.of(
                "missingSlots", intent.missingSlots(),
                "slots", intent.slots()
        ));
        String question = intent.clarifyQuestion() == null || intent.clarifyQuestion().isBlank()
                ? "请补充关键信息后我再继续处理。"
                : intent.clarifyQuestion();
        sendToken(emitter, conversationId, messageId, sequence, answerBuffer, question);
    }

    private void handleWaitingConfirm(
            SseEmitter emitter,
            String conversationId,
            String messageId,
            String message,
            ConversationSession session,
            StringBuilder answerBuffer,
            AtomicLong sequence
    ) {
        boolean confirmed = message.matches(".*(确认|同意|是|提交|继续).*");
        boolean canceled = message.matches(".*(取消|否|不用|停止).*");
        ConfirmationExecutionResult result;
        if (confirmed) {
            result = confirmationExecutionService.confirm(session.pendingOperationId());
        } else if (canceled) {
            result = confirmationExecutionService.cancel(session.pendingOperationId());
        } else {
            eventSender.send(emitter, "confirm_required", conversationId, messageId, sequence, Map.of(
                    "pendingOperationId", session.pendingOperationId(),
                    "summary", "请回复“确认”或“取消”。",
                    "actions", List.of("confirm", "cancel")
            ));
            sendToken(emitter, conversationId, messageId, sequence, answerBuffer, "请回复“确认”或“取消”。");
            sendDone(emitter, conversationId, messageId, sequence, answerBuffer, ConversationState.WAITING_CONFIRM, session.pendingOperationId());
            emitter.complete();
            return;
        }
        memoryService.updateState(conversationId, ConversationState.COMPLETED, null, Map.of(), List.of(), null);
        eventSender.send(emitter, "tool_result", conversationId, messageId, sequence, result.data());
        sendToken(emitter, conversationId, messageId, sequence, answerBuffer, result.message());
        sendDone(emitter, conversationId, messageId, sequence, answerBuffer, ConversationState.COMPLETED, null);
        emitter.complete();
    }

    private IntentRecognitionResult buildSlotFillingIntent(ConversationSession session, String message) {
        return IntentRecognitionResult.builder()
                .intent(session.pendingIntent() == null ? IntentType.AMBIGUOUS : session.pendingIntent())
                .confidence(0.7D)
                .slots(Map.of("userSupplement", message))
                .missingSlots(List.of())
                .routeTarget("SLOT_FILLING")
                .reason("补槽状态下直接合并用户补充")
                .build();
    }

    private String buildContext(ConversationSession session) {
        StringBuilder builder = new StringBuilder();
        if (session.history() == null) {
            return "";
        }
        for (ConversationMessageSnapshot item : session.history()) {
            builder.append(item.role()).append("：").append(item.content()).append("\n");
        }
        return builder.toString();
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

    private void sendToken(
            SseEmitter emitter,
            String conversationId,
            String messageId,
            AtomicLong sequence,
            StringBuilder answerBuffer,
            String token
    ) {
        answerBuffer.append(token);
        eventSender.send(emitter, "message", conversationId, messageId, sequence, Map.of("delta", token));
    }

    private void sendDone(
            SseEmitter emitter,
            String conversationId,
            String messageId,
            AtomicLong sequence,
            StringBuilder answerBuffer,
            ConversationState state,
            String pendingOperationId
    ) {
        eventSender.send(emitter, "done", conversationId, messageId, sequence, Map.of(
                "conversationId", conversationId,
                "messageId", messageId,
                "state", state == null ? ConversationState.NORMAL.name() : state.name(),
                "pendingOperationId", pendingOperationId == null ? "" : pendingOperationId,
                "answerLength", answerBuffer.length()
        ));
    }

    private String normalizeConversationId(String conversationId) {
        return conversationId == null || conversationId.isBlank() ? UUID.randomUUID().toString() : conversationId;
    }

    private String normalizeMessageId(String clientMessageId) {
        return clientMessageId == null || clientMessageId.isBlank() ? UUID.randomUUID().toString() : clientMessageId;
    }
}
