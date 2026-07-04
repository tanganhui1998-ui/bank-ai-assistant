package com.bank.aiassistant.tool;

import com.bank.aiassistant.business.ApprovalBusinessService;
import com.bank.aiassistant.business.LeaveBusinessService;
import com.bank.aiassistant.business.OutsourcingBusinessService;
import com.bank.aiassistant.business.SalaryBusinessService;
import com.bank.aiassistant.context.CurrentUser;
import com.bank.aiassistant.domain.enums.PendingOperationType;
import dev.langchain4j.agent.tool.P;
import dev.langchain4j.agent.tool.Tool;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.util.Arrays;
import java.util.Map;

/**
 * 银行业务 Function Calling 工具集合。
 *
 * READ 工具通过权限校验后直接返回查询结果；WRITE 工具只生成待确认操作，
 * 不直接执行业务写入，避免大模型误调用造成真实数据变更。
 */
@Component
@RequiredArgsConstructor
public class BankBusinessTools {

    private final BusinessToolPermissionService permissionService;
    private final ToolCallAuditService auditService;
    private final PendingConfirmationService pendingConfirmationService;
    private final LeaveBusinessService leaveBusinessService;
    private final SalaryBusinessService salaryBusinessService;
    private final ApprovalBusinessService approvalBusinessService;
    private final OutsourcingBusinessService outsourcingBusinessService;

    @Tool(name = "create_leave_application", value = {
            "当用户明确提出请假申请时调用，需要提供请假类型、开始时间、结束时间和请假原因。如果用户未提供完整参数，系统应追问。不适用于查询假期余额或请假记录。"
    })
    public ToolCallResult createLeaveApplication(
            @P(value = "请假类型，例如年假、事假、病假。必填。", required = true) String leaveType,
            @P(value = "请假开始时间，格式建议 yyyy-MM-dd HH:mm。必填。", required = true) String startTime,
            @P(value = "请假结束时间，格式建议 yyyy-MM-dd HH:mm。必填。", required = true) String endTime,
            @P(value = "请假原因。必填。", required = true) String reason
    ) {
        long start = System.currentTimeMillis();
        Map<String, Object> params = Map.of("leaveType", leaveType, "startTime", startTime, "endTime", endTime, "reason", reason);
        CurrentUser user = permissionService.currentUser();
        ToolCallResult result = withWritePermission("create_leave_application", null, () -> {
            ToolCallResult validation = require(params, "leaveType", "startTime", "endTime", "reason");
            if (validation != null) {
                return validation;
            }
            String pendingId = pendingConfirmationService.create(
                    user,
                    PendingOperationType.SUBMIT,
                    "创建请假单：" + leaveType + "，" + startTime + " 至 " + endTime,
                    params
            );
            return ToolCallResult.pendingConfirm("已生成请假申请待确认，请确认后提交。", pendingId, params);
        });
        audit(user, "create_leave_application", params, result, start);
        return result;
    }

    @Tool(name = "query_leave_balance", value = {
            "当用户询问剩余年假、事假、病假或假期余额时调用。不适用于创建请假单或查询请假历史记录。"
    })
    public ToolCallResult queryLeaveBalance() {
        long start = System.currentTimeMillis();
        CurrentUser user = permissionService.currentUser();
        Map<String, Object> params = Map.of("targetUserId", user.userId());
        ToolCallResult result = withReadPermission("query_leave_balance", user.userId(),
                () -> ToolCallResult.success("查询假期余额成功。", leaveBusinessService.queryLeaveBalance(user.userId())));
        audit(user, "query_leave_balance", params, result, start);
        return result;
    }

    @Tool(name = "query_leave_records", value = {
            "当用户查询历史请假列表、请假记录或请假审批状态时调用。不适用于查询假期余额，也不用于创建新的请假申请。"
    })
    public ToolCallResult queryLeaveRecords() {
        long start = System.currentTimeMillis();
        CurrentUser user = permissionService.currentUser();
        Map<String, Object> params = Map.of("targetUserId", user.userId());
        ToolCallResult result = withReadPermission("query_leave_records", user.userId(),
                () -> ToolCallResult.success("查询请假记录成功。", Map.of("records", leaveBusinessService.queryLeaveRecords(user.userId()))));
        audit(user, "query_leave_records", params, result, start);
        return result;
    }

    @Tool(name = "query_salary_detail", value = {
            "当用户按月份查询工资明细、工资构成、税前税后金额时调用，需要提供查询月份。不适用于按年份汇总薪资历史。"
    })
    public ToolCallResult querySalaryDetail(
            @P(value = "查询月份，格式 yyyy-MM，例如 2026-06。必填。", required = true) String month
    ) {
        long start = System.currentTimeMillis();
        CurrentUser user = permissionService.currentUser();
        Map<String, Object> params = Map.of("month", month);
        ToolCallResult result = withReadPermission("query_salary_detail", user.userId(), () -> {
            ToolCallResult validation = require(params, "month");
            if (validation != null) {
                return validation;
            }
            return ToolCallResult.success("查询薪资明细成功。", salaryBusinessService.querySalaryDetail(user.userId(), month));
        });
        audit(user, "query_salary_detail", params, result, start);
        return result;
    }

    @Tool(name = "query_salary_history", value = {
            "当用户按年份查询历史薪资汇总、全年工资趋势时调用，需要提供年份。不适用于查询某个月的工资明细。"
    })
    public ToolCallResult querySalaryHistory(
            @P(value = "查询年份，格式 yyyy，例如 2026。必填。", required = true) String year
    ) {
        long start = System.currentTimeMillis();
        CurrentUser user = permissionService.currentUser();
        Map<String, Object> params = Map.of("year", year);
        ToolCallResult result = withReadPermission("query_salary_history", user.userId(), () -> {
            ToolCallResult validation = require(params, "year");
            if (validation != null) {
                return validation;
            }
            return ToolCallResult.success("查询薪资历史成功。", Map.of("items", salaryBusinessService.querySalaryHistory(user.userId(), year)));
        });
        audit(user, "query_salary_history", params, result, start);
        return result;
    }

    @Tool(name = "query_approval_progress", value = {
            "当用户按业务单号查询审批进度、当前审批节点或处理人时调用，需要提供业务单号。不适用于查询个人待办列表。"
    })
    public ToolCallResult queryApprovalProgress(
            @P(value = "业务单号或流程单号。必填。", required = true) String businessNo
    ) {
        long start = System.currentTimeMillis();
        CurrentUser user = permissionService.currentUser();
        Map<String, Object> params = Map.of("businessNo", businessNo);
        ToolCallResult result = withReadPermission("query_approval_progress", user.userId(), () -> {
            ToolCallResult validation = require(params, "businessNo");
            if (validation != null) {
                return validation;
            }
            return ToolCallResult.success("查询审批进度成功。", approvalBusinessService.queryApprovalProgress(businessNo));
        });
        audit(user, "query_approval_progress", params, result, start);
        return result;
    }

    @Tool(name = "query_todo_tasks", value = {
            "当用户查询我的待办、待审批任务、需要我处理的审批列表时调用。不适用于查询某个业务单号的审批进度。"
    })
    public ToolCallResult queryTodoTasks() {
        long start = System.currentTimeMillis();
        CurrentUser user = permissionService.currentUser();
        Map<String, Object> params = Map.of("targetUserId", user.userId());
        ToolCallResult result = withReadPermission("query_todo_tasks", user.userId(),
                () -> ToolCallResult.success("查询待办列表成功。", Map.of("tasks", approvalBusinessService.queryTodoTasks(user.userId()))));
        audit(user, "query_todo_tasks", params, result, start);
        return result;
    }

    @Tool(name = "submit_approval_opinion", value = {
            "当用户明确要求对某笔业务提交审批意见时调用，需要业务单号、审批结论和审批意见。不适用于仅查询审批进度或查询待办列表。"
    })
    public ToolCallResult submitApprovalOpinion(
            @P(value = "业务单号或流程单号。必填。", required = true) String businessNo,
            @P(value = "审批结论，例如同意、驳回、退回补充。必填。", required = true) String decision,
            @P(value = "审批意见说明。必填。", required = true) String comment
    ) {
        long start = System.currentTimeMillis();
        CurrentUser user = permissionService.currentUser();
        Map<String, Object> params = Map.of("businessNo", businessNo, "decision", decision, "comment", comment);
        ToolCallResult result = withWritePermission("submit_approval_opinion", null, () -> {
            ToolCallResult validation = require(params, "businessNo", "decision", "comment");
            if (validation != null) {
                return validation;
            }
            String pendingId = pendingConfirmationService.create(user, PendingOperationType.APPROVE,
                    "提交审批意见：" + businessNo + "，结论：" + decision, params);
            return ToolCallResult.pendingConfirm("已生成审批意见待确认，请确认后提交。", pendingId, params);
        });
        audit(user, "submit_approval_opinion", params, result, start);
        return result;
    }

    @Tool(name = "create_outsourcing_entry_application", value = {
            "当用户明确提出创建外包人员入场申请时调用，需要人员姓名、供应商、入场日期、项目和材料清单。不适用于查询外包申请进度。"
    })
    public ToolCallResult createOutsourcingEntryApplication(
            @P(value = "外包人员姓名。必填。", required = true) String personName,
            @P(value = "供应商名称。必填。", required = true) String vendor,
            @P(value = "入场日期，格式建议 yyyy-MM-dd。必填。", required = true) String entryDate,
            @P(value = "入场所属项目名称。必填。", required = true) String project,
            @P(value = "材料清单，多个材料用逗号分隔。必填。", required = true) String materials
    ) {
        long start = System.currentTimeMillis();
        CurrentUser user = permissionService.currentUser();
        Map<String, Object> params = Map.of("personName", personName, "vendor", vendor, "entryDate", entryDate, "project", project, "materials", materials);
        ToolCallResult result = withWritePermission("create_outsourcing_entry_application", null, () -> {
            ToolCallResult validation = require(params, "personName", "vendor", "entryDate", "project", "materials");
            if (validation != null) {
                return validation;
            }
            String pendingId = pendingConfirmationService.create(user, PendingOperationType.SUBMIT,
                    "创建外包入场申请：" + personName + "，供应商：" + vendor, params);
            return ToolCallResult.pendingConfirm("已生成外包入场申请待确认，请确认后提交。", pendingId, params);
        });
        audit(user, "create_outsourcing_entry_application", params, result, start);
        return result;
    }

    @Tool(name = "query_outsourcing_application_progress", value = {
            "当用户按申请单号查询外包入场申请进度、当前节点或办理状态时调用，需要申请单号。不适用于创建新的外包入场申请。"
    })
    public ToolCallResult queryOutsourcingApplicationProgress(
            @P(value = "外包入场申请单号。必填。", required = true) String applicationNo
    ) {
        long start = System.currentTimeMillis();
        CurrentUser user = permissionService.currentUser();
        Map<String, Object> params = Map.of("applicationNo", applicationNo);
        ToolCallResult result = withReadPermission("query_outsourcing_application_progress", user.userId(), () -> {
            ToolCallResult validation = require(params, "applicationNo");
            if (validation != null) {
                return validation;
            }
            return ToolCallResult.success("查询外包申请进度成功。", outsourcingBusinessService.queryApplicationProgress(applicationNo));
        });
        audit(user, "query_outsourcing_application_progress", params, result, start);
        return result;
    }

    private ToolCallResult withReadPermission(String toolName, String targetUserId, ToolExecutor executor) {
        ToolCallResult denied = permissionService.check(toolName, ToolOperationType.READ, targetUserId);
        return denied != null ? denied : executor.execute();
    }

    private ToolCallResult withWritePermission(String toolName, String targetUserId, ToolExecutor executor) {
        ToolCallResult denied = permissionService.check(toolName, ToolOperationType.WRITE, targetUserId);
        return denied != null ? denied : executor.execute();
    }

    private ToolCallResult require(Map<String, Object> params, String... requiredKeys) {
        String missing = Arrays.stream(requiredKeys)
                .filter(key -> params.get(key) == null || !StringUtils.hasText(String.valueOf(params.get(key))))
                .findFirst()
                .orElse(null);
        return missing == null ? null : ToolCallResult.validationError("缺少必填参数：" + missing + "，请补充后再试。", Map.of("missingSlot", missing));
    }

    private void audit(CurrentUser user, String toolName, Map<String, Object> params, ToolCallResult result, long start) {
        auditService.record(user, toolName, params, result, System.currentTimeMillis() - start);
    }

    private interface ToolExecutor {
        ToolCallResult execute();
    }
}
