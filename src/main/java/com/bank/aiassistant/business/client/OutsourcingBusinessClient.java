package com.bank.aiassistant.business.client;

import com.bank.aiassistant.context.CurrentUser;

import java.util.Map;

/**
 * 外包管理业务系统 Client。
 */
public interface OutsourcingBusinessClient {

    Map<String, Object> queryApplicationProgress(CurrentUser user, String applicationNo);
}
