package com.bank.aiassistant.web;

import lombok.Builder;

import java.time.LocalDateTime;

@Builder
public record Result<T>(
        String code,
        String message,
        T data,
        LocalDateTime timestamp
) {

    public static <T> Result<T> success(T data) {
        return Result.<T>builder()
                .code("0")
                .message("success")
                .data(data)
                .timestamp(LocalDateTime.now())
                .build();
    }

    public static <T> Result<T> fail(String code, String message) {
        return Result.<T>builder()
                .code(code)
                .message(message)
                .timestamp(LocalDateTime.now())
                .build();
    }
}
