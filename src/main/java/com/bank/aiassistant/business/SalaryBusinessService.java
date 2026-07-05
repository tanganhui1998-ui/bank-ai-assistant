package com.bank.aiassistant.business;

import com.bank.aiassistant.business.client.SalaryBusinessClient;
import com.bank.aiassistant.context.CurrentUserProvider;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;

/**
 * 薪资业务服务。
 */
@Service
@RequiredArgsConstructor
public class SalaryBusinessService {

    private final CurrentUserProvider currentUserProvider;
    private final SalaryBusinessClient client;

    public Map<String, Object> querySalaryDetail(String userId, String month) {
        return client.querySalaryDetail(currentUserProvider.currentUser(), userId, month);
    }

    public List<Map<String, Object>> querySalaryHistory(String userId, String year) {
        return client.querySalaryHistory(currentUserProvider.currentUser(), userId, year);
    }
}
