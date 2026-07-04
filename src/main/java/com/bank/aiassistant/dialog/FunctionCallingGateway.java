package com.bank.aiassistant.dialog;

import java.util.Map;

/**
 * Function Calling 网关预留接口。
 *
 * 第九批会接入真实读/写工具；当前先返回可控占位结果，保证对话路由链路完整。
 */
public interface FunctionCallingGateway {

    ToolRouteResult query(String question, Map<String, Object> slots);

    ToolRouteResult execute(String question, Map<String, Object> slots);
}
