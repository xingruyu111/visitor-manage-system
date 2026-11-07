package com.visitor.security;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.visitor.common.core.Result;
import com.visitor.common.enums.RoleEnum;
import com.visitor.domain.entity.User;
import com.visitor.domain.entity.UserRole;
import com.visitor.server.mapper.UserMapper;
import com.visitor.server.mapper.UserRoleMapper;
import io.jsonwebtoken.Claims;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.stereotype.Component;
import org.springframework.web.method.HandlerMethod;
import org.springframework.web.servlet.HandlerInterceptor;

import java.util.List;

/**
 * 登录与角色校验：
 * 1) 校验 Bearer 令牌、黑名单、用户状态；
 * 2) 解析当前角色（可用 X-Current-Role 切换，必须在用户角色列表内）；
 * 3) 按路径做角色约束：/web/websuper/** 仅 SUPER，/web/admin/** 仅 ADMIN/SUPER。
 */
@Component
public class AuthInterceptor implements HandlerInterceptor {

    private final JwtUtil jwtUtil;
    private final TokenBlacklist tokenBlacklist;
    private final UserMapper userMapper;
    private final UserRoleMapper userRoleMapper;
    private final ObjectMapper objectMapper;

    public AuthInterceptor(JwtUtil jwtUtil, TokenBlacklist tokenBlacklist,
                           UserMapper userMapper, UserRoleMapper userRoleMapper,
                           ObjectMapper objectMapper) {
        this.jwtUtil = jwtUtil;
        this.tokenBlacklist = tokenBlacklist;
        this.userMapper = userMapper;
        this.userRoleMapper = userRoleMapper;
        this.objectMapper = objectMapper;
    }

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler)
            throws Exception {
        if (!(handler instanceof HandlerMethod)) {
            return true;
        }

        String authorization = request.getHeader("Authorization");
        if (authorization == null || !authorization.startsWith("Bearer ")) {
            return reject(response, 401, "未登录或登录已过期");
        }

        Claims claims;
        try {
            claims = jwtUtil.parse(authorization.substring(7));
        } catch (Exception e) {
            return reject(response, 401, "登录已过期，请重新登录");
        }

        if (tokenBlacklist.contains(claims.getId())) {
            return reject(response, 401, "登录已失效，请重新登录");
        }

        Long userId = Long.valueOf(claims.getSubject());
        User user = userMapper.selectById(userId);
        if (user == null || !Integer.valueOf(1).equals(user.getStatus())) {
            return reject(response, 403, "账号不存在或已停用");
        }

        List<UserRole> roles = userRoleMapper.selectList(new LambdaQueryWrapper<UserRole>()
                .eq(UserRole::getUserId, userId)
                .eq(UserRole::getStatus, 1)
                .orderByAsc(UserRole::getId));
        if (roles.isEmpty()) {
            return reject(response, 403, "账号未分配角色");
        }

        RoleEnum requested = parseRole(request.getHeader("X-Current-Role"));
        UserRole active = resolveActiveRole(roles, requested, claims);
        if (active == null) {
            return reject(response, 403, "当前角色无权限");
        }
        if (active.getRole().requiresSchool() && active.getSchoolId() == null) {
            return reject(response, 403, "缺少学校上下文");
        }

        String path = request.getRequestURI().substring(request.getContextPath().length());
        if (path.startsWith("/web/websuper/") && active.getRole() != RoleEnum.SUPER) {
            return reject(response, 403, "无权限访问该资源");
        }
        if (path.startsWith("/web/admin/") && active.getRole() != RoleEnum.ADMIN
                && active.getRole() != RoleEnum.SUPER) {
            return reject(response, 403, "无权限访问该资源");
        }

        UserContext.set(userId, user.getName(), active.getRole(), active.getSchoolId());
        return true;
    }

    @Override
    public void afterCompletion(HttpServletRequest request, HttpServletResponse response,
                                Object handler, Exception ex) {
        UserContext.clear();
    }

    private UserRole resolveActiveRole(List<UserRole> roles, RoleEnum requested, Claims claims) {
        if (requested != null) {
            return roles.stream().filter(role -> role.getRole() == requested).findFirst().orElse(null);
        }
        Integer tokenRole = claims.get("role", Integer.class);
        if (tokenRole != null) {
            RoleEnum roleEnum = RoleEnum.of(tokenRole);
            if (roleEnum != null) {
                UserRole matched = roles.stream().filter(role -> role.getRole() == roleEnum)
                        .findFirst().orElse(null);
                if (matched != null) {
                    return matched;
                }
            }
        }
        return roles.stream()
                .filter(role -> Integer.valueOf(1).equals(role.getIsDefault()))
                .findFirst()
                .orElse(roles.get(0));
    }

    private RoleEnum parseRole(String header) {
        if (header == null || header.isBlank()) {
            return null;
        }
        try {
            return RoleEnum.of(Integer.valueOf(header.trim()));
        } catch (NumberFormatException e) {
            return null;
        }
    }

    private boolean reject(HttpServletResponse response, int code, String message) throws Exception {
        response.setStatus(code);
        response.setCharacterEncoding("UTF-8");
        response.setContentType("application/json;charset=UTF-8");
        response.getWriter().write(objectMapper.writeValueAsString(Result.fail(code, message)));
        return false;
    }
}
