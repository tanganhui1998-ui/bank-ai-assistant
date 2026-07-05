package com.bank.aiassistant.business;

import com.bank.aiassistant.business.client.OutsourcingBusinessClient;
import com.bank.aiassistant.context.CurrentUserProvider;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.Map;

/**
 * 外包管理业务服务。
 */
@Service
@RequiredArgsConstructor
public class OutsourcingBusinessService {

    private final CurrentUserProvider currentUserProvider;
    private final OutsourcingBusinessClient client;

    public Map<String, Object> queryApplicationProgress(String applicationNo) {
        return client.queryApplicationProgress(currentUserProvider.currentUser(), applicationNo);
    }
}
