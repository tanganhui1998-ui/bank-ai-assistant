package com.bank.aiassistant.business.client;

import com.bank.aiassistant.context.CurrentUser;

import java.util.List;
import java.util.Map;

/**
 * 薪资业务系统 Client。
 */
public interface SalaryBusinessClient {

    Map<String, Object> querySalaryDetail(CurrentUser user, String targetUserId, String month);

    List<Map<String, Object>> querySalaryHistory(CurrentUser user, String targetUserId, String year);
}
