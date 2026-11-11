package com.visitor.domain.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("vst_visit_config")
public class VisitConfig {

    @TableId(type = IdType.AUTO)
    private Long id;
    private Long schoolId;
    private String alumniMonthsJson;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
