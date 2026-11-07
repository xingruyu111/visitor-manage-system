package com.visitor.security;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.visitor.common.enums.RoleEnum;
import com.visitor.domain.entity.User;
import com.visitor.domain.entity.UserRole;
import com.visitor.server.mapper.UserMapper;
import com.visitor.server.mapper.UserRoleMapper;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.web.method.HandlerMethod;

import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class AuthInterceptorTest {

    private static final String SECRET = "unit-test-secret-key-1234567890-abcdef";

    private final JwtUtil jwtUtil = new JwtUtil(SECRET, 60000);
    private final TokenBlacklist blacklist = mock(TokenBlacklist.class);
    private final UserMapper userMapper = mock(UserMapper.class);
    private final UserRoleMapper userRoleMapper = mock(UserRoleMapper.class);
    private final AuthInterceptor interceptor = new AuthInterceptor(jwtUtil, blacklist, userMapper,
            userRoleMapper, new ObjectMapper());

    private HttpServletRequest request;
    private HttpServletResponse response;
    private StringWriter body;
    private HandlerMethod handler;

    @BeforeEach
    void setUp() throws IOException {
        request = mock(HttpServletRequest.class);
        response = mock(HttpServletResponse.class);
        body = new StringWriter();
        when(response.getWriter()).thenReturn(new PrintWriter(body));
        when(request.getContextPath()).thenReturn("/api");
        when(request.getRequestURI()).thenReturn("/api/web/websuper/integration-configs");
        handler = mock(HandlerMethod.class);
    }

    @AfterEach
    void tearDown() {
        UserContext.clear();
    }

    @Test
    void nonControllerHandlerIsSkipped() throws Exception {
        assertThat(interceptor.preHandle(request, response, new Object())).isTrue();
    }

    @Test
    void missingAuthorizationIsRejected() throws Exception {
        assertThat(interceptor.preHandle(request, response, handler)).isFalse();
        assertThat(body.toString()).contains("未登录").contains("401");
    }

    @Test
    void malformedTokenIsRejected() throws Exception {
        when(request.getHeader("Authorization")).thenReturn("Bearer not-a-jwt");

        assertThat(interceptor.preHandle(request, response, handler)).isFalse();
        assertThat(body.toString()).contains("登录已过期");
    }

    @Test
    void blacklistedTokenIsRejected() throws Exception {
        String token = tokenFor(1L, RoleEnum.SYSTEM_ADMIN);
        when(request.getHeader("Authorization")).thenReturn("Bearer " + token);
        when(blacklist.contains(anyString())).thenReturn(true);

        assertThat(interceptor.preHandle(request, response, handler)).isFalse();
        assertThat(body.toString()).contains("登录已失效");
    }

    @Test
    void disabledUserIsRejected() throws Exception {
        String token = tokenFor(1L, RoleEnum.SYSTEM_ADMIN);
        when(request.getHeader("Authorization")).thenReturn("Bearer " + token);
        when(userMapper.selectById(1L)).thenReturn(user(1L, 0));

        assertThat(interceptor.preHandle(request, response, handler)).isFalse();
        assertThat(body.toString()).contains("账号不存在或已停用");
    }

    @Test
    void userWithoutRolesIsRejected() throws Exception {
        String token = tokenFor(1L, RoleEnum.SYSTEM_ADMIN);
        when(request.getHeader("Authorization")).thenReturn("Bearer " + token);
        when(userMapper.selectById(1L)).thenReturn(user(1L, 1));
        when(userRoleMapper.selectList(any())).thenReturn(List.of());

        assertThat(interceptor.preHandle(request, response, handler)).isFalse();
        assertThat(body.toString()).contains("未分配角色");
    }

    @Test
    void roleNotOwnedIsRejected() throws Exception {
        String token = tokenFor(1L, RoleEnum.SYSTEM_ADMIN);
        when(request.getHeader("Authorization")).thenReturn("Bearer " + token);
        when(userMapper.selectById(1L)).thenReturn(user(1L, 1));
        when(userRoleMapper.selectList(any())).thenReturn(List.of(roleRow(1L, RoleEnum.SYSTEM_ADMIN, null)));
        when(request.getHeader("X-Current-Role")).thenReturn(String.valueOf(RoleEnum.SECURITY_ADMIN.getCode()));

        assertThat(interceptor.preHandle(request, response, handler)).isFalse();
        assertThat(body.toString()).contains("当前角色无权限");
    }

    @Test
    void adminWithoutSchoolIsRejected() throws Exception {
        String token = tokenFor(2L, RoleEnum.SECURITY_ADMIN);
        when(request.getHeader("Authorization")).thenReturn("Bearer " + token);
        when(request.getRequestURI()).thenReturn("/api/web/admin/ping");
        when(userMapper.selectById(2L)).thenReturn(user(2L, 1));
        when(userRoleMapper.selectList(any())).thenReturn(List.of(roleRow(2L, RoleEnum.SECURITY_ADMIN, null)));

        assertThat(interceptor.preHandle(request, response, handler)).isFalse();
        assertThat(body.toString()).contains("缺少学校上下文");
    }

    @Test
    void adminCannotAccessWebsuperPath() throws Exception {
        String token = tokenFor(2L, RoleEnum.SECURITY_ADMIN);
        when(request.getHeader("Authorization")).thenReturn("Bearer " + token);
        when(userMapper.selectById(2L)).thenReturn(user(2L, 1));
        when(userRoleMapper.selectList(any())).thenReturn(List.of(roleRow(2L, RoleEnum.SECURITY_ADMIN, 10L)));

        assertThat(interceptor.preHandle(request, response, handler)).isFalse();
        assertThat(body.toString()).contains("无权限访问该资源");
    }

    @Test
    void superAdminPassesAndContextIsSet() throws Exception {
        String token = tokenFor(1L, RoleEnum.SYSTEM_ADMIN);
        when(request.getHeader("Authorization")).thenReturn("Bearer " + token);
        when(userMapper.selectById(1L)).thenReturn(user(1L, 1));
        when(userRoleMapper.selectList(any())).thenReturn(List.of(roleRow(1L, RoleEnum.SYSTEM_ADMIN, null)));

        assertThat(interceptor.preHandle(request, response, handler)).isTrue();
        assertThat(UserContext.getUserId()).isEqualTo(1L);
        assertThat(UserContext.getRole()).isEqualTo(RoleEnum.SYSTEM_ADMIN);

        interceptor.afterCompletion(request, response, handler, null);
        assertThat(UserContext.getUserId()).isNull();
    }

    private String tokenFor(Long userId, RoleEnum role) {
        return jwtUtil.generate(userId, "user-" + userId, List.of(role), role, null);
    }

    private User user(Long id, int status) {
        User user = new User();
        user.setId(id);
        user.setName("user-" + id);
        user.setStatus(status);
        return user;
    }

    private UserRole roleRow(Long userId, RoleEnum role, Long schoolId) {
        UserRole userRole = new UserRole();
        userRole.setUserId(userId);
        userRole.setRole(role);
        userRole.setSchoolId(schoolId);
        userRole.setIsDefault(1);
        userRole.setStatus(1);
        userRole.setCreatedAt(LocalDateTime.now());
        return userRole;
    }
}
