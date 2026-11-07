package com.visitor.domain.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("vst_form_field")
public class FormField {

    @TableId(type = IdType.AUTO)
    private Long id;
    private String visitorType;
    private String fieldKey;
    private String label;
    /** TEXT/NUMBER/DATE/SELECT/PHONE/ID_CARD */
    private String fieldType;
    private Integer required;
    private String options;
    private Integer sortNo;
    private Integer status;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
