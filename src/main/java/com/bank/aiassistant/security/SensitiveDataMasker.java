package com.bank.aiassistant.security;

import org.springframework.stereotype.Component;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;

/**
 * 审计参数脱敏器。
 *
 * 审计日志需要可追溯，但不能泄露敏感数据。薪资、身份证、银行卡、手机号等字段
 * 统一写入星号，避免日志系统成为新的敏感数据泄露面。
 */
@Component
public class SensitiveDataMasker {

    private static final Set<String> SENSITIVE_KEYS = Set.of(
            "salary", "salaryAmount", "baseSalary", "netSalary", "performance",
            "idCard", "identityNo", "cardNo", "bankCard", "mobile", "phone"
    );

    public Map<String, Object> mask(Map<String, Object> input) {
        if (input == null || input.isEmpty()) {
            return Map.of();
        }
        Map<String, Object> masked = new LinkedHashMap<>();
        input.forEach((key, value) -> {
            if (key != null && SENSITIVE_KEYS.stream().anyMatch(sensitive -> sensitive.equalsIgnoreCase(key))) {
                masked.put(key, "****");
            } else if (value instanceof Map<?, ?> nested) {
                masked.put(key, maskNested(nested));
            } else {
                masked.put(key, value);
            }
        });
        return masked;
    }

    private Map<String, Object> maskNested(Map<?, ?> nested) {
        Map<String, Object> copy = new LinkedHashMap<>();
        nested.forEach((key, value) -> copy.put(String.valueOf(key), value));
        return mask(copy);
    }
}
