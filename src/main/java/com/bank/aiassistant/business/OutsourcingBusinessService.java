package com.bank.aiassistant.business;

import org.springframework.stereotype.Service;

import java.util.Map;

/**
 * 外包管理业务查询服务 Mock 实现。
 */
@Service
public class OutsourcingBusinessService {

    public Map<String, Object> queryApplicationProgress(String applicationNo) {
        return Map.of(
                "applicationNo", applicationNo,
                "status", "材料复核中",
                "currentNode", "安全合规审核",
                "expectedEntryDate", "2026-07-15"
        );
    }
}
