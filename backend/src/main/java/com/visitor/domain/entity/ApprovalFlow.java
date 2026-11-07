package com.visitor.domain.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("vst_approval_flow")
public class ApprovalFlow {

    @TableId(type = IdType.AUTO)
    private Long id;
    private String flowCode;
    private String name;
    private Integer version;
    /** 0草稿 1已发布 2已停用 */
    private Integer status;
    private String definitionJson;
    private Long createdBy;
    private LocalDateTime publishedAt;
    private Long publishedBy;
    private String remark;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
