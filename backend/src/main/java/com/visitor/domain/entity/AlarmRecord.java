package com.visitor.domain.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("vst_alarm_record")
public class AlarmRecord {

    @TableId(type = IdType.AUTO)
    private Long id;
    private Long recordId;
    /** OVERSTAY */
    private String alarmType;
    private String message;
    private Integer handled;
    private Long handledBy;
    private LocalDateTime handledAt;
    private LocalDateTime createdAt;
}
