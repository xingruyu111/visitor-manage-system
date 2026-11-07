package com.visitor.common.enums;

import com.baomidou.mybatisplus.annotation.EnumValue;
import com.fasterxml.jackson.annotation.JsonValue;

/**
 * 人员类型：决定审批路由（学生→辅导员二级、附属学生→班主任等）。
 */
public enum PersonTypeEnum {

    TEACHER(1, "教职工", RoleEnum.TEACHER),
    STUDENT(2, "学生", RoleEnum.STUDENT),
    MERCHANT(3, "校内商户", RoleEnum.MERCHANT),
    CANTEEN(4, "食堂人员", RoleEnum.CANTEEN),
    AFFILIATED_TEACHER(5, "附属学校教职工", RoleEnum.TEACHER),
    AFFILIATED_STUDENT(6, "附属学校学生", RoleEnum.STUDENT),
    ALUMNI(7, "校友", RoleEnum.ALUMNI);

    @EnumValue
    private final int code;
    private final String description;
    private final RoleEnum role;

    PersonTypeEnum(int code, String description, RoleEnum role) {
        this.code = code;
        this.description = description;
        this.role = role;
    }

    @JsonValue
    public int getCode() {
        return code;
    }

    public String getDescription() {
        return description;
    }

    public RoleEnum getRole() {
        return role;
    }

    public static PersonTypeEnum of(Integer code) {
        if (code == null) {
            return null;
        }
        for (PersonTypeEnum value : values()) {
            if (value.code == code) {
                return value;
            }
        }
        return null;
    }

    public static PersonTypeEnum of(String name) {
        if (name == null || name.isBlank()) {
            return null;
        }
        try {
            return valueOf(name.trim().toUpperCase());
        } catch (IllegalArgumentException e) {
            return null;
        }
    }
}
