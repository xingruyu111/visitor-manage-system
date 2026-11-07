package com.visitor.domain.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("vst_department")
public class Department {

    @TableId(type = IdType.AUTO)
    private Long id;
    private Long schoolId;
    private Long parentId;
    private String name;
    private Integer level;
    private String ancestors;
    private Integer sortOrder;
    /** 数据中台部门编码 */
    private String externalId;
    private Integer status;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
