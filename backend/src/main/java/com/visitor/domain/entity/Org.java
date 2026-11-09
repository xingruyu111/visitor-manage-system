package com.visitor.domain.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("vst_org")
public class Org {

    @TableId(type = IdType.AUTO)
    private Long id;
    private Long parentId;
    private String name;
    private String ancestors;
    private Integer level;
    private Integer sortOrder;
    private Long headTeacherId;
    private String externalId;
    private Integer status;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
