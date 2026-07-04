package com.bank.aiassistant.domain.entity;

import com.bank.aiassistant.domain.enums.ConfidentialityLevel;
import com.bank.aiassistant.domain.enums.DocumentBusinessType;
import com.bank.aiassistant.domain.enums.DocumentProcessStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

/**
 * 文档主表对象。
 *
 * MyBatis 直接按字段名和数据库下划线列名映射，枚举统一以 name() 字符串入库。
 */
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Document {

    private String documentId;
    private String fileName;
    private Long fileSize;
    private String fileType;
    private String fileHash;
    private String ossBucket;
    private String ossObjectKey;
    private String ossEtag;
    private String accessUrl;
    private String displayName;
    private DocumentBusinessType documentType;
    private String versionNo;
    private String department;
    private LocalDateTime effectiveTime;
    private ConfidentialityLevel confidentialityLevel;
    private String applicableScope;
    private String publishingUnit;
    private DocumentProcessStatus processStatus;
    private String parseErrorMessage;
    private Integer retryCount;
    private String uploaderId;
    private String uploaderName;
    private LocalDateTime createdTime;
    private LocalDateTime updatedTime;
    private LocalDateTime publishedTime;
}
