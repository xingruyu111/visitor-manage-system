package com.visitor.security;

import com.visitor.common.enums.RoleEnum;

/**
 * 当前请求的登录上下文，由 AuthInterceptor 写入、请求结束后清理。
 */
public final class UserContext {

    private static final ThreadLocal<CurrentUser> HOLDER = new ThreadLocal<>();

    private UserContext() {
    }

    public static void set(Long userId, String name, RoleEnum role, Long schoolId) {
        HOLDER.set(new CurrentUser(userId, name, role, schoolId));
    }

    public static CurrentUser get() {
        return HOLDER.get();
    }

    public static Long getUserId() {
        CurrentUser current = HOLDER.get();
        return current == null ? null : current.userId();
    }

    public static RoleEnum getRole() {
        CurrentUser current = HOLDER.get();
        return current == null ? null : current.role();
    }

    public static Long getSchoolId() {
        CurrentUser current = HOLDER.get();
        return current == null ? null : current.schoolId();
    }

    public static String getName() {
        CurrentUser current = HOLDER.get();
        return current == null ? null : current.name();
    }

    public static void clear() {
        HOLDER.remove();
    }

    public record CurrentUser(Long userId, String name, RoleEnum role, Long schoolId) {
    }
}
