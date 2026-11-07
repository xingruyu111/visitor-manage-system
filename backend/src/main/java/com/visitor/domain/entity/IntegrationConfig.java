package com.visitor.domain.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 学校系统集成配置（六类：SSO/DATA_SYNC/GATE/FACE/PORTAL/TODO_MSG）。
 */
@Data
@TableName("vst_integration_config")
public class IntegrationConfig {

    @TableId(type = IdType.AUTO)
    private Long id;
    private String integrationKey;
    private String protocol;
    private Integer enabled;
    private String baseUrl;
    private String authType;
    private String appId;
    /** AES-GCM 密文，enc:v1: 前缀 */
    private String appSecret;
    private String endpoints;
    private String fieldMapping;
    private String extra;
    private Integer timeoutMs;
    private Integer retryTimes;
    private Integer status;
    private String remark;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private Long updatedBy;
}
