package com.bank.aiassistant.business;

import com.bank.aiassistant.business.client.ApprovalBusinessClient;
import com.bank.aiassistant.context.CurrentUserProvider;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;

/**
 * 审批业务服务。
 */
@Service
@RequiredArgsConstructor
public class ApprovalBusinessService {

    private final CurrentUserProvider currentUserProvider;
    private final ApprovalBusinessClient client;

    public Map<String, Object> queryApprovalProgress(String businessNo) {
        return client.queryApprovalProgress(currentUserProvider.currentUser(), businessNo);
    }

    public List<Map<String, Object>> queryTodoTasks(String userId) {
        return client.queryTodoTasks(currentUserProvider.currentUser(), userId);
    }
}
