package com.bank.aiassistant.tool;

import com.bank.aiassistant.web.Result;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 写操作二次确认接口。
 *
 * 前端不允许直接调用真实业务写接口，只能通过这里确认或取消 AI 生成的待确认指令。
 */
@Slf4j
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/assistant/confirm")
public class ConfirmationController {

    private final ConfirmationExecutionService confirmationExecutionService;

    @PostMapping("/{pendingId}")
    public Result<ConfirmationExecutionResult> confirm(@PathVariable String pendingId) {
        log.info("Received confirm request, pendingId={}", pendingId);
        return Result.success(confirmationExecutionService.confirm(pendingId));
    }

    @PostMapping("/{pendingId}/cancel")
    public Result<ConfirmationExecutionResult> cancel(@PathVariable String pendingId) {
        log.info("Received cancel confirmation request, pendingId={}", pendingId);
        return Result.success(confirmationExecutionService.cancel(pendingId));
    }

    @PostMapping
    public Result<ConfirmationExecutionResult> submit(@Valid @RequestBody ConfirmationRequest request) {
        log.info("Received confirmation card request, pendingId={}, action={}", request.getPendingId(), request.getAction());
        if ("cancel".equalsIgnoreCase(request.getAction())) {
            return Result.success(confirmationExecutionService.cancel(request.getPendingId()));
        }
        return Result.success(confirmationExecutionService.confirm(request.getPendingId()));
    }
}
