package com.bank.aiassistant.business.client;

import com.bank.aiassistant.business.BusinessExecutionResult;
import com.bank.aiassistant.config.BusinessIntegrationProperties;
import com.bank.aiassistant.context.CurrentUser;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

import java.util.List;
import java.util.Map;

/**
 * HTTP 业务系统 Client。
 *
 * 真实业务系统接入时建议由网关做服务鉴权，本 Client 负责透传当前用户身份，
 * 方便下游系统做本人、部门、数据范围和审计校验。
 */
@Slf4j
@Service
@ConditionalOnProperty(prefix = "app.business-integration", name = "enabled", havingValue = "true")
public class HttpBusinessSystemClient implements LeaveBusinessClient, SalaryBusinessClient,
        ApprovalBusinessClient, OutsourcingBusinessClient, BusinessWriteClient {

    private static final ParameterizedTypeReference<Map<String, Object>> MAP_TYPE = new ParameterizedTypeReference<>() {
    };
    private static final ParameterizedTypeReference<List<Map<String, Object>>> LIST_TYPE = new ParameterizedTypeReference<>() {
    };

    private final BusinessIntegrationProperties properties;
    private final RestClient restClient;

    public HttpBusinessSystemClient(BusinessIntegrationProperties properties) {
        this.properties = properties;
        SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
        factory.setConnectTimeout(properties.getConnectTimeoutMillis());
        factory.setReadTimeout(properties.getReadTimeoutMillis());
        this.restClient = RestClient.builder()
                .baseUrl(properties.getBaseUrl())
                .requestFactory(factory)
                .build();
    }

    @Override
    public Map<String, Object> queryLeaveBalance(CurrentUser user, String targetUserId) {
        return getMap(user, properties.getLeaveBalancePath(), Map.of("targetUserId", targetUserId));
    }

    @Override
    public List<Map<String, Object>> queryLeaveRecords(CurrentUser user, String targetUserId) {
        return getList(user, properties.getLeaveRecordsPath(), Map.of("targetUserId", targetUserId));
    }

    @Override
    public Map<String, Object> querySalaryDetail(CurrentUser user, String targetUserId, String month) {
        return getMap(user, properties.getSalaryDetailPath(), Map.of("targetUserId", targetUserId, "month", month));
    }

    @Override
    public List<Map<String, Object>> querySalaryHistory(CurrentUser user, String targetUserId, String year) {
        return getList(user, properties.getSalaryHistoryPath(), Map.of("targetUserId", targetUserId, "year", year));
    }

    @Override
    public Map<String, Object> queryApprovalProgress(CurrentUser user, String businessNo) {
        return getMap(user, properties.getApprovalProgressPath(), Map.of("businessNo", businessNo));
    }

    @Override
    public List<Map<String, Object>> queryTodoTasks(CurrentUser user, String targetUserId) {
        return getList(user, properties.getTodoTasksPath(), Map.of("targetUserId", targetUserId));
    }

    @Override
    public Map<String, Object> queryApplicationProgress(CurrentUser user, String applicationNo) {
        return getMap(user, properties.getOutsourcingProgressPath(), Map.of("applicationNo", applicationNo));
    }

    @Override
    public BusinessExecutionResult execute(CurrentUser user, String toolName, Map<String, Object> params) {
        Map<String, Object> response = restClient.post()
                .uri(properties.getExecutePath())
                .headers(headers -> appendIdentityHeaders(headers, user))
                .body(Map.of("toolName", toolName, "params", params))
                .retrieve()
                .body(MAP_TYPE);
        Map<String, Object> data = unwrapMap(response);
        return BusinessExecutionResult.builder()
                .success(booleanValue(data.getOrDefault("success", true)))
                .businessOrderNo(stringValue(data.get("businessOrderNo")))
                .message(stringValue(data.getOrDefault("message", "业务系统已受理")))
                .data(data)
                .build();
    }

    private Map<String, Object> getMap(CurrentUser user, String path, Map<String, Object> params) {
        Map<String, Object> response = restClient.get()
                .uri(builder -> {
                    builder.path(path);
                    params.forEach(builder::queryParam);
                    return builder.build();
                })
                .headers(headers -> appendIdentityHeaders(headers, user))
                .retrieve()
                .body(MAP_TYPE);
        return unwrapMap(response);
    }

    private List<Map<String, Object>> getList(CurrentUser user, String path, Map<String, Object> params) {
        Object response = restClient.get()
                .uri(builder -> {
                    builder.path(path);
                    params.forEach(builder::queryParam);
                    return builder.build();
                })
                .headers(headers -> appendIdentityHeaders(headers, user))
                .retrieve()
                .body(Object.class);
        Object data = response instanceof Map<?, ?> map && map.containsKey("data") ? map.get("data") : response;
        if (data instanceof List<?> list) {
            return list.stream()
                    .filter(Map.class::isInstance)
                    .map(item -> (Map<String, Object>) item)
                    .toList();
        }
        log.warn("Business list response is not a list, path={}, response={}", path, response);
        return List.of();
    }

    @SuppressWarnings("unchecked")
    private Map<String, Object> unwrapMap(Map<String, Object> response) {
        if (response == null) {
            return Map.of();
        }
        Object data = response.get("data");
        return data instanceof Map<?, ?> ? (Map<String, Object>) data : response;
    }

    private void appendIdentityHeaders(org.springframework.http.HttpHeaders headers, CurrentUser user) {
        headers.set("X-User-Id", user.userId());
        headers.set("X-User-Name", user.userName());
        headers.set("X-Tenant-Id", user.tenantId());
        headers.set("X-User-Roles", String.join(",", user.roles()));
        headers.set("X-User-Departments", String.join(",", user.departments()));
        headers.set("X-Data-Scope", user.dataScope());
        headers.set("X-Branch-No", user.branchNo());
        if (properties.getApiKey() != null && !properties.getApiKey().isBlank()) {
            headers.set("X-Api-Key", properties.getApiKey());
        }
    }

    private boolean booleanValue(Object value) {
        return value instanceof Boolean bool ? bool : Boolean.parseBoolean(String.valueOf(value));
    }

    private String stringValue(Object value) {
        return value == null ? "" : String.valueOf(value);
    }
}
