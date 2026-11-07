package com.visitor.domain.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import lombok.Data;

/**
 * 保存集成配置的入参。appSecret 为空表示“不修改已有密钥”。
 */
@Data
public class IntegrationConfigSaveDTO {

    private String protocol;
    private Boolean enabled;
    private String baseUrl;
    private String authType;
    private String appId;
    private String appSecret;
    private String endpoints;
    private String fieldMapping;
    private String extra;

    @Min(value = 100, message = "超时时间不能小于 100ms")
    @Max(value = 60000, message = "超时时间不能大于 60000ms")
    private Integer timeoutMs;

    @Min(value = 0, message = "重试次数不能小于 0")
    @Max(value = 5, message = "重试次数不能大于 5")
    private Integer retryTimes;

    private Integer status;
    private String remark;
}
