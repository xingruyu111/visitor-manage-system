package com.visitor.workflow;

/**
 * 审批人类型：TARGET 被访人、COUNSELOR 辅导员、HEAD_TEACHER 班主任、SECURITY 保卫、DEFAULT 默认审核人。
 */
public enum AssigneeType {

    TARGET,
    COUNSELOR,
    HEAD_TEACHER,
    SECURITY,
    DEFAULT;

    public static AssigneeType of(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        try {
            return valueOf(value.trim().toUpperCase());
        } catch (IllegalArgumentException e) {
            return null;
        }
    }
}
