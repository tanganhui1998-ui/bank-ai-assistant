package com.bank.aiassistant.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.MDC;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.UUID;

/**
 * 请求追踪过滤器。
 *
 * 每个请求都会获得 traceId，并写回响应头，便于前端、网关、后端日志和审计表串联排查。
 */
@Component
public class RequestTraceFilter extends OncePerRequestFilter {

    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain
    ) throws ServletException, IOException {
        String traceId = resolveTraceId(request);
        RequestTraceContext.set(traceId);
        MDC.put("traceId", traceId);
        response.setHeader(RequestTraceContext.TRACE_ID_HEADER, traceId);
        try {
            filterChain.doFilter(request, response);
        } finally {
            MDC.remove("traceId");
            RequestTraceContext.clear();
        }
    }

    private String resolveTraceId(HttpServletRequest request) {
        String header = request.getHeader(RequestTraceContext.TRACE_ID_HEADER);
        return StringUtils.hasText(header) ? header : UUID.randomUUID().toString();
    }
}
