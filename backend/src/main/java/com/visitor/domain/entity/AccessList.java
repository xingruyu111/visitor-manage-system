package com.visitor.domain.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("vst_access_list")
public class AccessList {

    @TableId(type = IdType.AUTO)
    private Long id;
    private Long schoolId;
    /** 1黑名单 2白名单 */
    private Integer listType;
    /** PERSON / PLATE_MOTOR / PLATE_EBIKE */
    private String targetType;
    private String targetValue;
    private String personName;
    private String plateNumber;
    private LocalDateTime effectiveFrom;
    private LocalDateTime effectiveTo;
    private String reason;
    private String source;
    private Integer status;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
