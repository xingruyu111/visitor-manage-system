package com.visitor.domain.entity;

import com.baomidou.mybatisplus.annotation.IdType;
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
    private Long deptId;
    private Long classId;
    private String name;
    private String phone;
    private PersonTypeEnum personType;
    private String jobNo;
    private String studentNo;
    private String position;
    private Long counselorId;
    private Integer status;
    /** LOCAL / SYNC / IMPORT */
    private String syncSource;
    private String externalId;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
