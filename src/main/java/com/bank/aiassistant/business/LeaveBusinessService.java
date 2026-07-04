package com.bank.aiassistant.business;

import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;

/**
 * 请假业务查询服务 Mock 实现。
 */
@Service
public class LeaveBusinessService {

    public Map<String, Object> queryLeaveBalance(String userId) {
        return Map.of(
                "userId", userId,
                "annualLeaveDays", 8.5,
                "personalLeaveDays", 3,
                "sickLeaveDays", 10
        );
    }

    public List<Map<String, Object>> queryLeaveRecords(String userId) {
        return List.of(
                Map.of("leaveType", "年假", "startTime", "2026-03-04 09:00", "endTime", "2026-03-05 18:00", "status", "已通过"),
                Map.of("leaveType", "病假", "startTime", "2026-05-12 09:00", "endTime", "2026-05-12 18:00", "status", "审批中")
        );
    }
}
