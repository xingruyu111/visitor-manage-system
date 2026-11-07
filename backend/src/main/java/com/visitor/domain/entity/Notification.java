package com.visitor.domain.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("vst_notification")
public class Notification {

    @TableId(type = IdType.AUTO)
    private Long id;
    private Long userId;
    private String title;
    private String content;
    /** SYSTEM / APPROVAL / NOTICE */
    private String type;
    private String bizType;
    private Long bizId;
    private Integer isRead;
    private LocalDateTime createdAt;
}
