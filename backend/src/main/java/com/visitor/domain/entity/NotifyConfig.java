package com.visitor.domain.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("vst_notify_config")
public class NotifyConfig {

    @TableId(type = IdType.AUTO)
    private Long id;
    /** APPROVAL_RESULT / EXPIRE_REMINDER / ACCESS_ALERT / BLACKLIST_ALERT */
    private String notifyType;
    private Integer enabled;
    /** SITE,MP,TODO */
    private String channels;
    private LocalDateTime updatedAt;
}
