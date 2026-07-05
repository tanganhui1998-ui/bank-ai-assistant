package com.bank.aiassistant.business.client;

import com.bank.aiassistant.business.BusinessExecutionResult;
import com.bank.aiassistant.context.CurrentUser;

import java.util.Map;

/**
 * 写操作业务系统 Client。
 *
 * AI 助手只在用户二次确认后调用该接口，真实业务系统仍需做最终权限和业务规则校验。
 */
public interface BusinessWriteClient {

    BusinessExecutionResult execute(CurrentUser user, String toolName, Map<String, Object> params);
}
