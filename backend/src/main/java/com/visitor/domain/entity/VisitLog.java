package com.visitor.domain.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("vst_visit_log")
public class VisitLog {

    @TableId(type = IdType.AUTO)
    private Long id;
    private Long schoolId;
    private Long recordId;
    private Long passId;
    /** IN/OUT */
    private String direction;
    /** LOCAL/GATE/FACE */
    private String source;
    private String plateNumber;
    private Long guardId;
    private LocalDateTime eventTime;
    private LocalDateTime createdAt;
}
