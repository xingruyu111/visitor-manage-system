package com.visitor.domain.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("vst_audit_log")
public class AuditLog {

    @TableId(type = IdType.AUTO)
    private Long id;
    private Long userId;
    private String userName;
    /** VISITOR_APPLY / APPROVAL_APPROVE / APPROVAL_REJECT / ACCESS_VERIFY / ACCESS_LIST_* */
    private String action;
    private String bizType;
    private Long bizId;
    private String detail;
    private String ip;
    private LocalDateTime createdAt;
}
