package com.visitor.domain.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("vst_approval_task")
public class ApprovalTask {

    @TableId(type = IdType.AUTO)
    private Long id;
    private Long instanceId;
    private Integer seq;
    private String nodeCode;
    private String nodeName;
    private String assigneeType;
    private Long assigneeId;
    /** PENDING / APPROVED / REJECTED / EXPIRED / SKIPPED */
    private String status;
    private String decision;
    private String comment;
    private Integer emergency;
    private LocalDateTime dueAt;
    private LocalDateTime actedAt;
    private Long actedBy;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
