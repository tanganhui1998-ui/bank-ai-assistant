package com.bank.aiassistant.security;

import org.springframework.stereotype.Component;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Pattern;

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
            "idCard", "identityNo", "cardNo", "bankCard", "mobile", "phone",
            "password", "token", "apiKey", "accessKey", "secret", "credential"
    );

    private static final Pattern MOBILE_PATTERN = Pattern.compile("(?<!\\d)1[3-9]\\d{9}(?!\\d)");
    private static final Pattern ID_CARD_PATTERN = Pattern.compile("(?<!\\d)\\d{6}(18|19|20)\\d{2}\\d{2}\\d{2}\\d{3}[0-9Xx](?!\\d)");
    private static final Pattern BANK_CARD_PATTERN = Pattern.compile("(?<!\\d)\\d{16,19}(?!\\d)");

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
            } else if (value instanceof List<?> list) {
                masked.put(key, list.stream().map(this::maskObject).toList());
            } else {
                masked.put(key, maskObject(value));
            }
        });
        return masked;
    }

    public Object maskObject(Object value) {
        if (value instanceof Map<?, ?> nested) {
            return maskNested(nested);
        }
        if (value instanceof List<?> list) {
            return list.stream().map(this::maskObject).toList();
        }
        if (value instanceof String text) {
            return maskText(text);
        }
        return value;
    }

    private Map<String, Object> maskNested(Map<?, ?> nested) {
        Map<String, Object> copy = new LinkedHashMap<>();
        nested.forEach((key, value) -> copy.put(String.valueOf(key), value));
        return mask(copy);
    }

    private String maskText(String text) {
        String masked = MOBILE_PATTERN.matcher(text).replaceAll("***********");
        masked = ID_CARD_PATTERN.matcher(masked).replaceAll("******************");
        masked = BANK_CARD_PATTERN.matcher(masked).replaceAll("****************");
        return masked;
    }
}
