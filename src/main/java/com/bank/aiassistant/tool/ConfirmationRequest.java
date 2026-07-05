package com.bank.aiassistant.tool;

import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.Setter;

/**
 * 前端确认卡片提交请求。
 */
@Getter
@Setter
public class ConfirmationRequest {

    @NotBlank(message = "pendingId cannot be blank")
    private String pendingId;

    /**
     * 操作动作：confirm 或 cancel。
     */
    private String action = "confirm";

    /**
     * 前端透传的确认备注，当前仅用于审计扩展预留。
     */
    private String comment;
}
