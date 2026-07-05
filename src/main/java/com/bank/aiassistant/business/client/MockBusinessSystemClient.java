package com.bank.aiassistant.business.client;

import com.bank.aiassistant.business.BusinessExecutionResult;
import com.bank.aiassistant.context.CurrentUser;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * 本地 Mock 业务系统 Client。
 *
 * 当没有真实 HR、薪资、审批、外包系统时使用，保证 AI 助手主流程可运行。
 */
@Service
@ConditionalOnProperty(prefix = "app.business-integration", name = "enabled", havingValue = "false", matchIfMissing = true)
public class MockBusinessSystemClient implements LeaveBusinessClient, SalaryBusinessClient,
        ApprovalBusinessClient, OutsourcingBusinessClient, BusinessWriteClient {

    @Override
    public Map<String, Object> queryLeaveBalance(CurrentUser user, String targetUserId) {
        return Map.of(
                "userId", targetUserId,
                "annualLeaveDays", 8.5,
                "personalLeaveDays", 3,
                "sickLeaveDays", 10
        );
    }

    @Override
    public List<Map<String, Object>> queryLeaveRecords(CurrentUser user, String targetUserId) {
        return List.of(
                Map.of("leaveType", "年假", "startTime", "2026-03-04 09:00", "endTime", "2026-03-05 18:00", "status", "已通过"),
                Map.of("leaveType", "病假", "startTime", "2026-05-12 09:00", "endTime", "2026-05-12 18:00", "status", "审批中")
        );
    }

    @Override
    public Map<String, Object> querySalaryDetail(CurrentUser user, String targetUserId, String month) {
        return Map.of(
                "userId", targetUserId,
                "month", month,
                "baseSalary", 18000,
                "performance", 5200,
                "allowance", 1200,
                "tax", 2100,
                "netSalary", 22300
        );
    }

    @Override
    public List<Map<String, Object>> querySalaryHistory(CurrentUser user, String targetUserId, String year) {
        return List.of(
                Map.of("year", year, "month", year + "-01", "netSalary", 22100),
                Map.of("year", year, "month", year + "-02", "netSalary", 22450),
                Map.of("year", year, "month", year + "-03", "netSalary", 22300)
        );
    }

    @Override
    public Map<String, Object> queryApprovalProgress(CurrentUser user, String businessNo) {
        return Map.of(
                "businessNo", businessNo,
                "currentNode", "部门负责人审批",
                "status", "审批中",
                "handler", "张经理"
        );
    }

    @Override
    public List<Map<String, Object>> queryTodoTasks(CurrentUser user, String targetUserId) {
        return List.of(
                Map.of("taskId", "T20260704001", "title", "请假审批", "applicant", "李四"),
                Map.of("taskId", "T20260704002", "title", "外包入场审批", "applicant", "王五")
        );
    }

    @Override
    public Map<String, Object> queryApplicationProgress(CurrentUser user, String applicationNo) {
        return Map.of(
                "applicationNo", applicationNo,
                "status", "材料复核中",
                "currentNode", "安全合规审核",
                "expectedEntryDate", "2026-07-15"
        );
    }

    @Override
    public BusinessExecutionResult execute(CurrentUser user, String toolName, Map<String, Object> params) {
        String businessOrderNo = "AI-" + UUID.randomUUID().toString().replace("-", "").substring(0, 16);
        return BusinessExecutionResult.builder()
                .success(true)
                .businessOrderNo(businessOrderNo)
                .message("Mock 业务系统已受理：" + toolName)
                .data(Map.of("toolName", toolName, "businessOrderNo", businessOrderNo, "params", params))
                .build();
    }
}
