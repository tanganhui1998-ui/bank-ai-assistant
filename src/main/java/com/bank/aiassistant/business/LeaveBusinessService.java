package com.bank.aiassistant.business;

import com.bank.aiassistant.business.client.LeaveBusinessClient;
import com.bank.aiassistant.context.CurrentUserProvider;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;

/**
 * 请假业务服务。
 *
 * 工具层只依赖该服务；真实系统调用和本地 Mock 由 LeaveBusinessClient 决定。
 */
@Service
@RequiredArgsConstructor
public class LeaveBusinessService {

    private final CurrentUserProvider currentUserProvider;
    private final LeaveBusinessClient client;

    public Map<String, Object> queryLeaveBalance(String userId) {
        return client.queryLeaveBalance(currentUserProvider.currentUser(), userId);
    }

    public List<Map<String, Object>> queryLeaveRecords(String userId) {
        return client.queryLeaveRecords(currentUserProvider.currentUser(), userId);
    }
}
