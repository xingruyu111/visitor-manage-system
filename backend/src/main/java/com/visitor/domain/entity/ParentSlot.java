package com.visitor.domain.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;

@Data
@TableName("vst_parent_slot")
public class ParentSlot {

    @TableId(type = IdType.AUTO)
    private Long id;
    private String visitorPhone;
    /** 1-7，空表示每天 */
    private Integer weekday;
    private LocalTime startTime;
    private LocalTime endTime;
    private LocalDate effectiveFrom;
    private LocalDate effectiveTo;
    private Integer status;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
