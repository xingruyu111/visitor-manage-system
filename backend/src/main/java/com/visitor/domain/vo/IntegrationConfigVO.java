package com.visitor.domain.vo;

import lombok.Data;

import java.time.LocalDateTime;

/**
 * 集成配置出参：密钥永远脱敏，只回显是否已配置。
 */
@Data
public class IntegrationConfigVO {

    private String integrationKey;
    private String description;
    private String protocol;
    private Integer enabled;
    private String baseUrl;
    private String authType;
    private String appId;
    /** 固定为 ****** 或 null */
    private String appSecret;
    private Boolean secretConfigured;
    private String endpoints;
    private String fieldMapping;
    private String extra;
    private Integer timeoutMs;
    private Integer retryTimes;
    private Integer status;
    private String remark;
    private LocalDateTime updatedAt;
}
