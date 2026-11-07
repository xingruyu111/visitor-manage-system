package com.visitor.common.enums;

import com.baomidou.mybatisplus.annotation.EnumValue;
import com.fasterxml.jackson.annotation.JsonValue;

/**
 * 角色编码：1–5 与既有系统保持一致，6–9 为高校场景扩展。
 */
public enum RoleEnum {

    SUPER(1, "超级管理员"),
    ADMIN(2, "学校管理员"),
    GUARD(3, "门卫"),
    VISITOR(4, "访客"),
    TEACHER(5, "教职工"),
    STUDENT(6, "学生"),
    MERCHANT(7, "校内商户"),
    CANTEEN(8, "食堂人员"),
    ALUMNI(9, "校友");

    @EnumValue
    private final int code;
    private final String description;

    RoleEnum(int code, String description) {
        this.code = code;
        this.description = description;
    }

    @JsonValue
    public int getCode() {
        return code;
    }

    public String getDescription() {
        return description;
    }

    public static RoleEnum of(Integer code) {
        if (code == null) {
            return null;
        }
        for (RoleEnum value : values()) {
            if (value.code == code) {
                return value;
            }
        }
        return null;
    }

    public static RoleEnum of(String name) {
        if (name == null || name.isBlank()) {
            return null;
        }
        try {
            return valueOf(name.trim().toUpperCase());
        } catch (IllegalArgumentException e) {
            return null;
        }
    }

    /** 是否需要绑定学校（超级管理员与访客除外）。 */
    public boolean requiresSchool() {
        return this != SUPER && this != VISITOR && this != ALUMNI;
    }
}
