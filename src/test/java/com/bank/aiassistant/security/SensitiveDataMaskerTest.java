package com.bank.aiassistant.security;

import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class SensitiveDataMaskerTest {

    private final SensitiveDataMasker masker = new SensitiveDataMasker();

    @Test
    void maskShouldHideSensitiveFieldsIgnoringCase() {
        Map<String, Object> result = masker.mask(Map.of(
                "salaryAmount", "20000",
                "PHONE", "13800000000",
                "department", "科技部"));

        assertThat(result)
                .containsEntry("salaryAmount", "****")
                .containsEntry("PHONE", "****")
                .containsEntry("department", "科技部");
    }

    @Test
    void maskShouldHideSensitiveFieldsInNestedMap() {
        Map<String, Object> result = masker.mask(Map.of(
                "employee", Map.of(
                        "idCard", "110101199001011234",
                        "name", "张三")));

        assertThat(result.get("employee"))
                .isInstanceOf(Map.class)
                .asInstanceOf(org.assertj.core.api.InstanceOfAssertFactories.MAP)
                .containsEntry("idCard", "****")
                .containsEntry("name", "张三");
    }
}
