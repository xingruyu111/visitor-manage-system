package com.visitor.domain.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("vst_class")
public class SchoolClass {

    @TableId(type = IdType.AUTO)
    private Long id;
    private Long schoolId;
    private String name;
    private String grade;
    private Long headTeacherId;
    private Long counselorId;
    /** 数据中台班级编码 */
    private String externalId;
    private Integer status;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
