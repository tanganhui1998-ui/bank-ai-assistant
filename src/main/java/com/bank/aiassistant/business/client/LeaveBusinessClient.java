package com.bank.aiassistant.business.client;

import com.bank.aiassistant.context.CurrentUser;

import java.util.List;
import java.util.Map;

/**
 * 请假业务系统 Client。
 */
public interface LeaveBusinessClient {

    Map<String, Object> queryLeaveBalance(CurrentUser user, String targetUserId);

    List<Map<String, Object>> queryLeaveRecords(CurrentUser user, String targetUserId);
}
