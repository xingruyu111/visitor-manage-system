package com.visitor.domain.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("vst_mp_config")
public class MpConfig {

    @TableId(type = IdType.AUTO)
    private Long id;
    private String appId;
    private String secret;
    private String auditTemplateId;
    private String auditResultTemplateId;
    /** MOCK / REST */
    private String authMode;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
