package com.visitor.domain.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("vst_user")
public class User {

    @TableId(type = IdType.AUTO)
    private Long id;
    private String phone;
    private String name;
    /** BCrypt 散列 */
    private String password;
    private String idCard;
    private String avatarUrl;
    private String mpOpenid;
    private String unionId;
    private Integer status;
    private LocalDateTime lastLoginTime;
    private String lastLoginIp;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
