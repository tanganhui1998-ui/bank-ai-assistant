package com.bank.aiassistant.dialog;

import com.bank.aiassistant.tool.BankBusinessTools;
import com.bank.aiassistant.tool.ToolCallResult;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.time.YearMonth;
import java.util.Map;

/**
 * Function Calling 网关实现。
 *
 * 当前版本根据意图识别得到的 slots 和用户问题做轻量路由，调用具体 @Tool 方法。
 * 后续如果接入 LangChain4j Tool Calling，可以复用 BankBusinessTools 中的工具定义，
 * 让大模型自动选择工具和参数。
 */
@Component
@RequiredArgsConstructor
public class BusinessFunctionCallingGateway implements FunctionCallingGateway {

    private final BankBusinessTools tools;

    @Override
    public ToolRouteResult query(String question, Map<String, Object> slots) {
        ToolCallResult result;
        if (containsAny(question, "假期余额", "剩余年假", "年假余额", "病假天数", "事假天数")) {
            result = tools.queryLeaveBalance();
        } else if (containsAny(question, "请假记录", "请假历史", "请假列表")) {
            result = tools.queryLeaveRecords();
        } else if (containsAny(question, "薪资明细", "工资明细", "工资构成")) {
            result = tools.querySalaryDetail(slot(slots, "month", YearMonth.now().toString()));
        } else if (containsAny(question, "薪资历史", "工资历史", "全年工资")) {
            result = tools.querySalaryHistory(slot(slots, "year", String.valueOf(YearMonth.now().getYear())));
        } else if (containsAny(question, "待办", "待审批")) {
            result = tools.queryTodoTasks();
        } else if (containsAny(question, "外包", "入场") && containsAny(question, "进度", "状态")) {
            result = tools.queryOutsourcingApplicationProgress(slot(slots, "applicationNo", ""));
        } else {
            result = tools.queryApprovalProgress(slot(slots, "businessNo", ""));
        }
        return toRouteResult(result);
    }

    @Override
    public ToolRouteResult execute(String question, Map<String, Object> slots) {
        ToolCallResult result;
        if (containsAny(question, "请假", "休假")) {
            result = tools.createLeaveApplication(
                    slot(slots, "leaveType", ""),
                    slot(slots, "startTime", ""),
                    slot(slots, "endTime", ""),
                    slot(slots, "reason", "")
            );
        } else if (containsAny(question, "审批", "同意", "驳回")) {
            result = tools.submitApprovalOpinion(
                    slot(slots, "businessNo", ""),
                    slot(slots, "decision", ""),
                    slot(slots, "comment", "")
            );
        } else {
            result = tools.createOutsourcingEntryApplication(
                    slot(slots, "personName", ""),
                    slot(slots, "vendor", ""),
                    slot(slots, "entryDate", ""),
                    slot(slots, "project", ""),
                    slot(slots, "materials", "")
            );
        }
        return toRouteResult(result);
    }

    private ToolRouteResult toRouteResult(ToolCallResult result) {
        return ToolRouteResult.builder()
                .answer(result.message())
                .data(result.data())
                .pendingOperationId(result.pendingOperationId())
                .waitingConfirm(result.pendingOperationId() != null && !result.pendingOperationId().isBlank())
                .build();
    }

    private boolean containsAny(String text, String... keywords) {
        if (text == null) {
            return false;
        }
        for (String keyword : keywords) {
            if (text.contains(keyword)) {
                return true;
            }
        }
        return false;
    }

    private String slot(Map<String, Object> slots, String key, String defaultValue) {
        if (slots == null || slots.get(key) == null) {
            return defaultValue;
        }
        String value = String.valueOf(slots.get(key));
        return value.isBlank() ? defaultValue : value;
    }
}
