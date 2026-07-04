package com.bank.aiassistant.business;

import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;

/**
 * 审批业务查询服务 Mock 实现。
 */
@Service
public class ApprovalBusinessService {

    public Map<String, Object> queryApprovalProgress(String businessNo) {
        return Map.of(
                "businessNo", businessNo,
                "currentNode", "部门负责人审批",
                "status", "审批中",
                "handler", "张经理"
        );
    }

    public List<Map<String, Object>> queryTodoTasks(String userId) {
        return List.of(
                Map.of("taskId", "T20260704001", "title", "请假审批", "applicant", "李四"),
                Map.of("taskId", "T20260704002", "title", "外包入场审批", "applicant", "王五")
        );
    }
}
