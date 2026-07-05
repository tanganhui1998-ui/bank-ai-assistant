package com.bank.aiassistant.retrieval;

import com.bank.aiassistant.context.CurrentUser;
import com.bank.aiassistant.context.CurrentUserProvider;
import com.bank.aiassistant.domain.entity.RetrievalFeedback;
import com.bank.aiassistant.repository.RetrievalFeedbackRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;

/**
 * RAG 检索反馈服务。
 *
 * 用户反馈是后续 RAG 效果增强的闭环入口：低分问题可以进入知识补全池，
 * 高频差评问题可以用于扩充评测集和同义词词典。
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class RetrievalFeedbackService {

    private final CurrentUserProvider currentUserProvider;
    private final RetrievalFeedbackRepository repository;

    public RetrievalFeedbackResponse submit(RetrievalFeedbackRequest request) {
        CurrentUser user = currentUserProvider.currentUser();
        RetrievalFeedback feedback = repository.save(RetrievalFeedback.builder()
                .userId(user.userId())
                .userName(user.userName())
                .conversationId(request.getConversationId())
                .question(request.getQuestion())
                .helpful(request.getHelpful())
                .rating(request.getRating())
                .comment(request.getComment())
                .chunkIds(String.join(",", safeList(request.getChunkIds())))
                .createdTime(LocalDateTime.now())
                .build());
        log.info("RAG feedback submitted, feedbackId={}, userId={}, question={}, rating={}, helpful={}",
                feedback.getFeedbackId(), user.userId(), request.getQuestion(), request.getRating(), request.getHelpful());
        return RetrievalFeedbackResponse.builder()
                .feedbackId(feedback.getFeedbackId())
                .status("RECORDED")
                .build();
    }

    private List<String> safeList(List<String> values) {
        return values == null ? List.of() : values;
    }
}
