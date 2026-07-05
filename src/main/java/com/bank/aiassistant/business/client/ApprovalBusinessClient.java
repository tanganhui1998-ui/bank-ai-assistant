package com.bank.aiassistant.business.client;

import com.bank.aiassistant.context.CurrentUser;

import java.util.List;
import java.util.Map;

/**
 * 审批业务系统 Client。
 */
public interface ApprovalBusinessClient {

    Map<String, Object> queryApprovalProgress(CurrentUser user, String businessNo);

    List<Map<String, Object>> queryTodoTasks(CurrentUser user, String targetUserId);
}
