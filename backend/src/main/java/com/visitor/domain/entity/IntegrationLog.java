package com.visitor.domain.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 集成调用日志，记录每次外部调用的结果与耗时。
 */
@Data
@TableName("vst_integration_log")
public class IntegrationLog {

    @TableId(type = IdType.AUTO)
    private Long id;
    private String integrationKey;
    private String action;
    private String bizType;
    private String bizId;
    /** OUT 出站调用 / IN 入站回调 */
    private String direction;
    private String requestDigest;
    private Integer httpStatus;
    private Integer success;
    private String errorCode;
    private String errorMsg;
    private Integer costMs;
    private String traceId;
    private LocalDateTime createdAt;
}
