package com.visitor.domain.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.FieldStrategy;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.visitor.common.enums.PersonTypeEnum;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("vst_staff")
public class Staff {

    @TableId(type = IdType.AUTO)
    private Long id;
    private Long schoolId;
    @TableField(updateStrategy = FieldStrategy.ALWAYS)
    private Long deptId;
    @TableField(updateStrategy = FieldStrategy.ALWAYS)
    private Long classId;
    private String name;
    private String phone;
    private String personNo;
    @TableField(updateStrategy = FieldStrategy.ALWAYS)
    private Long orgId;
    private PersonTypeEnum personType;

    /** 仅用于展示，不落库 */
    @TableField(exist = false)
    private String orgName;
    private String jobNo;
    @TableField(updateStrategy = FieldStrategy.ALWAYS)
    private String studentNo;
    @TableField(updateStrategy = FieldStrategy.ALWAYS)
    private String position;
    @TableField(updateStrategy = FieldStrategy.ALWAYS)
    private Long counselorId;
    private Integer status;
    /** 0正常 1已删除 */
    private Integer isDeleted;
    /** LOCAL / SYNC / IMPORT */
    private String syncSource;
    private String externalId;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
