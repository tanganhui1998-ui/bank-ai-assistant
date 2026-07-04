package com.bank.aiassistant.document.dto;

import com.bank.aiassistant.domain.enums.ConfidentialityLevel;
import com.bank.aiassistant.domain.enums.DocumentBusinessType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

@Getter
@Setter
public class DocumentMetadataRequest {

    @NotBlank(message = "文档名称不能为空")
    @Size(max = 255, message = "文档名称不能超过255个字符")
    private String displayName;

    @NotNull(message = "文档类型不能为空")
    private DocumentBusinessType documentType;

    @Size(max = 64, message = "版本号不能超过64个字符")
    private String versionNo;

    @Size(max = 128, message = "所属部门不能超过128个字符")
    private String department;

    private LocalDateTime effectiveTime;

    private ConfidentialityLevel confidentialityLevel = ConfidentialityLevel.INTERNAL;

    @Size(max = 512, message = "适用范围不能超过512个字符")
    private String applicableScope;

    @Size(max = 128, message = "发布单位不能超过128个字符")
    private String publishingUnit;
}
