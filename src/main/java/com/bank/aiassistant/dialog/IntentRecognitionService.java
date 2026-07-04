package com.bank.aiassistant.dialog;

import com.bank.aiassistant.config.DialogProperties;
import com.bank.aiassistant.config.QwenProperties;
import com.bank.aiassistant.context.CurrentUser;
import com.bank.aiassistant.llm.QwenChatCompletionService;
import com.bank.aiassistant.llm.QwenChatMessage;
import com.bank.aiassistant.security.SecurityAuditEvent;
import com.bank.aiassistant.security.SecurityAuditService;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;

/**
 * 意图识别服务。
 *
 * 优先调用大模型输出结构化 JSON；超时、异常或 JSON 解析失败时，使用关键词规则兜底。
 * 兜底默认策略是 POLICY_QA，避免误触发业务办理写操作。
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class IntentRecognitionService {

    private final DialogProperties dialogProperties;
    private final QwenProperties qwenProperties;
    private final QwenChatCompletionService chatCompletionService;
    private final ObjectMapper objectMapper;
    private final ConversationMemoryService conversationMemoryService;
    private final SecurityAuditService securityAuditService;

    public IntentRecognitionResult recognize(String sessionId, String question, String context, CurrentUser user) {
        long start = System.currentTimeMillis();
        IntentRecognitionResult cached = conversationMemoryService.getCachedIntent(sessionId, question);
        if (cached != null) {
            log.info("Intent cache hit, userId={}, sessionId={}, intent={}, confidence={}",
                    user.userId(), sessionId, cached.intent(), cached.confidence());
            return cached;
        }

        try {
            String prompt = dialogProperties.getIntentPromptTemplate()
                    .replace("{context}", context == null ? "" : context)
                    .replace("{question}", question);
            String content = chatCompletionService.chat(
                    List.of(new QwenChatMessage("user", prompt)),
                    qwenProperties.getChat().getIntentTimeoutMillis()
            );
            IntentRecognitionResult result = parseResult(content, false);
            conversationMemoryService.cacheIntent(sessionId, question, result);
            logIntent(user, question, result, System.currentTimeMillis() - start);
            return result;
        } catch (Exception ex) {
            IntentRecognitionResult fallback = fallback(question);
            conversationMemoryService.cacheIntent(sessionId, question, fallback);
            log.warn("Intent recognition failed and fallback used, userId={}, question={}, fallbackIntent={}",
                    user.userId(), question, fallback.intent(), ex);
            logIntent(user, question, fallback, System.currentTimeMillis() - start);
            return fallback;
        }
    }

    private IntentRecognitionResult parseResult(String content, boolean fallback) throws Exception {
        String json = extractJson(content);
        Map<String, Object> map = objectMapper.readValue(json, new TypeReference<>() {
        });
        IntentType intent = IntentType.valueOf(String.valueOf(map.getOrDefault("intent", "POLICY_QA")));
        double confidence = Double.parseDouble(String.valueOf(map.getOrDefault("confidence", "0.7")));
        Map<String, Object> slots = map.get("slots") instanceof Map<?, ?> rawSlots
                ? objectMapper.convertValue(rawSlots, new TypeReference<>() {
        }) : Map.of();
        List<String> missingSlots = map.get("missingSlots") instanceof List<?> rawList
                ? rawList.stream().map(String::valueOf).toList()
                : List.of();
        return IntentRecognitionResult.builder()
                .intent(intent)
                .confidence(confidence)
                .slots(slots)
                .missingSlots(missingSlots)
                .clarifyQuestion(stringValue(map.get("clarifyQuestion")))
                .routeTarget(stringValue(map.get("routeTarget")))
                .reason(stringValue(map.get("reason")))
                .fallback(fallback)
                .build();
    }

    private IntentRecognitionResult fallback(String question) {
        String lower = question.toLowerCase();
        IntentType intent;
        if (question.matches(".*(办理|申请|提交|审批|修改|删除|发起|报销|请假).*")) {
            intent = IntentType.BUSINESS_EXECUTE;
        } else if (question.matches(".*(查询|统计|进度|余额|记录|明细|多少|几次).*")) {
            intent = IntentType.BUSINESS_QUERY;
        } else if (question.matches(".*(你好|谢谢|你是谁|帮助|能做什么).*") || lower.matches(".*(hello|hi|thanks).*")) {
            intent = IntentType.CHITCHAT;
        } else {
            intent = IntentType.POLICY_QA;
        }
        return IntentRecognitionResult.builder()
                .intent(intent)
                .confidence(0.61D)
                .slots(Map.of())
                .missingSlots(List.of())
                .routeTarget(intent.name())
                .reason("规则兜底识别")
                .fallback(true)
                .build();
    }

    private String extractJson(String content) {
        int start = content.indexOf('{');
        int end = content.lastIndexOf('}');
        if (start >= 0 && end > start) {
            return content.substring(start, end + 1);
        }
        return content;
    }

    private void logIntent(CurrentUser user, String question, IntentRecognitionResult result, long elapsedMs) {
        boolean lowConfidence = result.confidence() < dialogProperties.getLowConfidenceThreshold();
        log.info("Intent recognized, userId={}, question={}, intent={}, confidence={}, elapsedMs={}, routeTarget={}, lowConfidence={}",
                user.userId(), question, result.intent(), result.confidence(), elapsedMs, result.routeTarget(), lowConfidence);
        securityAuditService.record(SecurityAuditEvent.builder()
                .user(user)
                .actionType("INTENT_RECOGNIZED")
                .operationType(result.intent() == null ? null : result.intent().name())
                .inputParams(Map.of("question", question))
                .result(Map.of(
                        "intent", result.intent() == null ? "" : result.intent().name(),
                        "confidence", result.confidence(),
                        "routeTarget", result.routeTarget() == null ? "" : result.routeTarget(),
                        "fallback", result.fallback()
                ))
                .rejected(false)
                .elapsedMs(elapsedMs)
                .build());
        if (lowConfidence) {
            log.warn("Low confidence intent recognition, userId={}, question={}, intent={}, confidence={}",
                    user.userId(), question, result.intent(), result.confidence());
        }
    }

    private String stringValue(Object value) {
        return value == null ? "" : String.valueOf(value);
    }
}
