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

    @Test
    void maskShouldHideSensitiveValuesInsideTextAndLists() {
        Map<String, Object> result = masker.mask(Map.of(
                "comment", "手机号13800000000，身份证110101199001011234",
                "items", java.util.List.of(Map.of("bankCard", "6222020202020202020"))));

        assertThat(result.get("comment")).isEqualTo("手机号***********，身份证******************");
        assertThat(result.get("items"))
                .asInstanceOf(org.assertj.core.api.InstanceOfAssertFactories.LIST)
                .first()
                .asInstanceOf(org.assertj.core.api.InstanceOfAssertFactories.MAP)
                .containsEntry("bankCard", "****");
    }
}
