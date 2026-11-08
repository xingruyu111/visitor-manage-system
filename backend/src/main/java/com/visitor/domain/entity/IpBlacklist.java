package com.visitor.domain.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("vst_ip_blacklist")
public class IpBlacklist {

    @TableId(type = IdType.AUTO)
    private Long id;
    private String ip;
    private String reason;
    /** NULL 表示永久 */
    private LocalDateTime expireAt;
    private Long createdBy;
    private LocalDateTime createdAt;
}
