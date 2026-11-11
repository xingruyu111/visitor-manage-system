package com.visitor.domain.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("vst_visitor_type")
public class VisitorType {

    @TableId(type = IdType.AUTO)
    private Long id;
    private String code;
    private String name;
    /** 0 表示不限次 */
    private Integer maxEntries;
    private String bookingRulesJson;
    private Integer status;
    private Integer sortNo;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
