package com.visitor.domain.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("vst_approval_instance")
public class ApprovalInstance {

    @TableId(type = IdType.AUTO)
    private Long id;
    private String bizType;
    private Long bizId;
    private Long schoolId;
    private String flowCode;
    private Integer flowVersion;
    /** 创建时的流程快照 */
    private String definitionJson;
    /** RUNNING / APPROVED / REJECTED / CANCELLED */
    private String status;
    private Integer currentSeq;
    private Long applicantId;
    private String visitorType;
    private String targetType;
    private Long targetId;
    private LocalDateTime startedAt;
    private LocalDateTime endedAt;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
