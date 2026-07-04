package com.bank.aiassistant.security;

import co.elastic.clients.elasticsearch._types.FieldValue;
import co.elastic.clients.elasticsearch._types.query_dsl.Query;
import com.bank.aiassistant.context.CurrentUser;
import com.bank.aiassistant.context.CurrentUserProvider;
import com.bank.aiassistant.domain.enums.ConfidentialityLevel;
import com.bank.aiassistant.domain.enums.DocumentProcessStatus;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 检索权限过滤服务。
 *
 * 所有知识库 ES 查询都应通过这里追加过滤条件，避免业务代码遗漏密级、
 * 部门、发布状态和最新版本限制。
 */
@Service
@RequiredArgsConstructor
public class PermissionFilterService {

    private final CurrentUserProvider currentUserProvider;

    public List<ConfidentialityLevel> accessibleConfidentialityLevels() {
        CurrentUser currentUser = currentUserProvider.currentUser();
        List<ConfidentialityLevel> levels = new ArrayList<>();
        levels.add(ConfidentialityLevel.PUBLIC);
        levels.add(ConfidentialityLevel.INTERNAL);
        if (hasRole(currentUser, "CONFIDENTIAL_ACCESS") || isAdmin(currentUser)) {
            levels.add(ConfidentialityLevel.CONFIDENTIAL);
        }
        if (hasRole(currentUser, "SECRET_ACCESS") || isAdmin(currentUser)) {
            levels.add(ConfidentialityLevel.SECRET);
        }
        return levels;
    }

    public List<String> accessibleDepartments() {
        CurrentUser currentUser = currentUserProvider.currentUser();
        return isAdmin(currentUser) ? List.of() : currentUser.departments();
    }

    /**
     * 构建 ES 查询层过滤条件。
     *
     * 自动过滤：
     * - 仅检索已发布文档。
     * - 仅检索最新版本。
     * - 根据用户角色过滤密级。
     * - 非管理员按部门过滤；未指定部门的文档视为全行可见。
     */
    public List<Query> buildEsFilterQueries() {
        CurrentUser currentUser = currentUserProvider.currentUser();
        List<Query> filters = new ArrayList<>();
        filters.add(Query.of(q -> q.term(t -> t.field("status").value(DocumentProcessStatus.PUBLISHED.name()))));
        filters.add(Query.of(q -> q.term(t -> t.field("latestVersion").value(true))));
        filters.add(Query.of(q -> q.terms(t -> t
                .field("confidentialityLevel")
                .terms(v -> v.value(accessibleConfidentialityLevels().stream()
                        .map(level -> FieldValue.of(level.name()))
                        .toList())))));

        if (!isAdmin(currentUser)) {
            List<FieldValue> departments = currentUser.departments().stream()
                    .map(FieldValue::of)
                    .toList();
            filters.add(Query.of(q -> q.bool(b -> {
                b.minimumShouldMatch("1");
                b.should(s -> s.bool(inner -> inner.mustNot(m -> m.exists(e -> e.field("department")))));
                if (!departments.isEmpty()) {
                    b.should(s -> s.terms(t -> t.field("department").terms(v -> v.value(departments))));
                }
                return b;
            })));
        }
        return filters;
    }

    /**
     * 构建原生 ES JSON 查询使用的过滤条件。
     *
     * Java Client Query 用于类型安全查询；在线混合检索需要同时使用 knn/highlight，
     * 这里提供 Map 形式，交给 ObjectMapper 序列化为 ES 原生 DSL。
     */
    public List<Map<String, Object>> buildEsFilterDsl() {
        CurrentUser currentUser = currentUserProvider.currentUser();
        List<Map<String, Object>> filters = new ArrayList<>();
        filters.add(term("status", DocumentProcessStatus.PUBLISHED.name()));
        filters.add(term("latestVersion", true));
        filters.add(terms("confidentialityLevel", accessibleConfidentialityLevels().stream()
                .map(Enum::name)
                .toList()));

        if (!isAdmin(currentUser)) {
            List<Map<String, Object>> should = new ArrayList<>();
            should.add(Map.of("bool", Map.of("must_not", List.of(Map.of("exists", Map.of("field", "department"))))));
            if (currentUser.departments() != null && !currentUser.departments().isEmpty()) {
                should.add(terms("department", currentUser.departments()));
            }
            filters.add(Map.of("bool", Map.of("should", should, "minimum_should_match", 1)));
        }
        return filters;
    }

    private Map<String, Object> term(String field, Object value) {
        return Map.of("term", Map.of(field, value));
    }

    private Map<String, Object> terms(String field, List<?> values) {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put(field, values);
        return Map.of("terms", body);
    }

    public boolean isAdmin(CurrentUser currentUser) {
        return hasRole(currentUser, "ADMIN");
    }

    private boolean hasRole(CurrentUser currentUser, String role) {
        return currentUser.roles() != null && currentUser.roles().contains(role);
    }
}
