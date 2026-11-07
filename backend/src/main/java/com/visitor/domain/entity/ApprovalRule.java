package com.visitor.domain.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("vst_approval_rule")
public class ApprovalRule {

    @TableId(type = IdType.AUTO)
    private Long id;
    private Long schoolId;
    private String visitorType;
    private String targetType;
    private String flowCode;
    private Integer priority;
    private Integer status;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
