package com.bank.aiassistant.security;

/**
 * 请求追踪上下文。
 *
 * 使用 ThreadLocal 保存 traceId，便于 Controller、审计和日志在同一请求内关联。
 */
public final class RequestTraceContext {

    public static final String TRACE_ID_HEADER = "X-Trace-Id";
    private static final ThreadLocal<String> TRACE_ID = new ThreadLocal<>();

    private RequestTraceContext() {
    }

    public static void set(String traceId) {
        TRACE_ID.set(traceId);
    }

    public static String get() {
        return TRACE_ID.get();
    }

    public static void clear() {
        TRACE_ID.remove();
    }
}
