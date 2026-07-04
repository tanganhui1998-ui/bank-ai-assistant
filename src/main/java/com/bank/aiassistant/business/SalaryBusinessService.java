package com.bank.aiassistant.business;

import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;

/**
 * 薪资业务查询服务 Mock 实现。
 */
@Service
public class SalaryBusinessService {

    public Map<String, Object> querySalaryDetail(String userId, String month) {
        return Map.of(
                "userId", userId,
                "month", month,
                "baseSalary", 18000,
                "performance", 5200,
                "allowance", 1200,
                "tax", 2100,
                "netSalary", 22300
        );
    }

    public List<Map<String, Object>> querySalaryHistory(String userId, String year) {
        return List.of(
                Map.of("year", year, "month", year + "-01", "netSalary", 22100),
                Map.of("year", year, "month", year + "-02", "netSalary", 22450),
                Map.of("year", year, "month", year + "-03", "netSalary", 22300)
        );
    }
}
