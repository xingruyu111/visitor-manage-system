package com.visitor.domain.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.visitor.common.enums.RoleEnum;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("vst_user_role")
public class UserRole {

    @TableId(type = IdType.AUTO)
    private Long id;
    private Long userId;
    private RoleEnum role;
    private Long schoolId;
    private Integer isDefault;
    private Integer status;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
