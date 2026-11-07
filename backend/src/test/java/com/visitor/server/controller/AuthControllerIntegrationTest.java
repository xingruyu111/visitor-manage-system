package com.visitor.server.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.visitor.common.enums.RoleEnum;
import com.visitor.domain.entity.IntegrationConfig;
import com.visitor.domain.entity.User;
import com.visitor.domain.entity.UserRole;
import com.visitor.server.mapper.IntegrationConfigMapper;
import com.visitor.server.mapper.UserMapper;
import com.visitor.server.mapper.UserRoleMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
class AuthControllerIntegrationTest {

    private static final String PASSWORD = "pass1234";

    @LocalServerPort
    private int port;

    @Autowired
    private TestRestTemplate rest;

    @Autowired
    private UserMapper userMapper;

    @Autowired
    private UserRoleMapper userRoleMapper;

    @Autowired
    private IntegrationConfigMapper integrationConfigMapper;

    @Autowired
    private PasswordEncoder passwordEncoder;

    private Long superUserId;
    private Long adminUserId;
    private Long noSchoolAdminId;
    private Long multiRoleUserId;

    @BeforeEach
    void setUp() {
        userRoleMapper.delete(null);
        userMapper.delete(null);
        integrationConfigMapper.delete(null);

        superUserId = createUser("13700000010", "超级管理员", 1, RoleEnum.SUPER, null);
        adminUserId = createUser("13700000011", "学校管理员", 1, RoleEnum.ADMIN, 1L);
        noSchoolAdminId = createUser("13700000012", "无校管理员", 1, RoleEnum.ADMIN, null);
        multiRoleUserId = createUser("13700000013", "多角色", 1, RoleEnum.SUPER, null);
        addRole(multiRoleUserId, RoleEnum.VISITOR, null, 0);
    }

    @Test
    void loginReturnsTokenAndRoles() {
        Map<String, Object> body = login("13700000010", PASSWORD);

        assertThat(body.get("code")).isEqualTo(200);
        Map<String, Object> data = data(body);
        assertThat(data.get("token")).asString().isNotBlank();
        assertThat(data.get("currentRole")).isEqualTo(1);
        assertThat((List<?>) data.get("roles")).hasSize(1);
    }

    @Test
    void loginWithWrongPasswordIsRejected() {
        Map<String, Object> body = login("13700000010", "wrong");

        assertThat(body.get("code")).isEqualTo(401);
        assertThat(body.get("message")).isEqualTo("账号或密码错误");
    }

    @Test
    void disabledAccountCannotLogin() {
        User user = userMapper.selectById(superUserId);
        user.setStatus(0);
        userMapper.updateById(user);

        Map<String, Object> body = login("13700000010", PASSWORD);

        assertThat(body.get("code")).isEqualTo(403);
    }

    @Test
    void meRequiresToken() {
        ResponseEntity<String> response = rest.getForEntity(url("/auth/me"), String.class);

        assertThat(response.getStatusCode().value()).isEqualTo(401);
    }

    @Test
    void meReturnsCurrentUser() {
        String token = token(login("13700000010", PASSWORD));

        ResponseEntity<String> response = get("/auth/me", token);

        assertThat(response.getStatusCode().value()).isEqualTo(200);
        assertThat(response.getBody()).contains("超级管理员").contains("\"currentRole\":1");
    }

    @Test
    void superAdminCanAccessWebsuperApis() {
        String token = token(login("13700000010", PASSWORD));

        ResponseEntity<String> response = get("/web/websuper/integration-configs", token);

        assertThat(response.getStatusCode().value()).isEqualTo(200);
    }

    @Test
    void adminCannotAccessWebsuperApis() {
        String token = token(login("13700000011", PASSWORD));

        ResponseEntity<String> response = get("/web/websuper/integration-configs", token);

        assertThat(response.getStatusCode().value()).isEqualTo(403);
    }

    @Test
    void adminWithoutSchoolIsRejected() {
        String token = token(login("13700000012", PASSWORD));

        ResponseEntity<String> response = get("/web/admin/ping", token);

        assertThat(response.getStatusCode().value()).isEqualTo(403);
        assertThat(response.getBody()).contains("缺少学校上下文");
    }

    @Test
    void adminWithSchoolCanAccessAdminApis() {
        String token = token(login("13700000011", PASSWORD));

        ResponseEntity<String> response = get("/web/admin/ping", token);

        assertThat(response.getStatusCode().value()).isEqualTo(200);
        assertThat(response.getBody()).contains("\"schoolId\":1");
    }

    @Test
    void switchingToRoleNotOwnedIsRejected() {
        String token = token(login("13700000010", PASSWORD));

        HttpHeaders headers = bearer(token);
        headers.set("X-Current-Role", String.valueOf(RoleEnum.ADMIN.getCode()));
        ResponseEntity<String> response = rest.exchange(url("/web/websuper/integration-configs"),
                HttpMethod.GET, new HttpEntity<>(headers), String.class);

        assertThat(response.getStatusCode().value()).isEqualTo(403);
    }

    @Test
    void canSwitchToOwnedRole() {
        String token = token(login("13700000013", PASSWORD));

        HttpHeaders headers = bearer(token);
        headers.set("X-Current-Role", String.valueOf(RoleEnum.VISITOR.getCode()));
        ResponseEntity<String> response = rest.exchange(url("/web/websuper/integration-configs"),
                HttpMethod.GET, new HttpEntity<>(headers), String.class);

        assertThat(response.getStatusCode().value()).isEqualTo(403);
    }

    @Test
    void logoutSucceeds() {
        String token = token(login("13700000010", PASSWORD));

        ResponseEntity<String> response = rest.exchange(url("/auth/logout"), HttpMethod.POST,
                new HttpEntity<>(bearer(token)), String.class);

        assertThat(response.getStatusCode().value()).isEqualTo(200);
    }

    @Test
    void meWithInvalidTokenIsRejected() {
        ResponseEntity<String> response = get("/auth/me", "not-a-jwt");

        assertThat(response.getStatusCode().value()).isEqualTo(401);
    }

    @Test
    void ssoCallbackFailsWhenNotConfigured() {
        ResponseEntity<String> response = rest.getForEntity(
                url("/auth/sso/callback?code=abc&redirectUri=http://localhost/callback"), String.class);

        assertThat(response.getStatusCode().value()).isEqualTo(200);
        assertThat(response.getBody()).contains("\"code\":400").contains("统一身份认证未配置");
    }

    @Test
    void ssoMockFlowIssuesTokenAndCreatesUser() {
        insertSsoConfig("MOCK");

        ResponseEntity<String> authorize = rest.getForEntity(
                url("/auth/sso/authorize?redirectUri=http://localhost/callback"), String.class);
        assertThat(authorize.getStatusCode().value()).isEqualTo(200);
        assertThat(authorize.getBody()).contains("mock://sso/authorize");

        ResponseEntity<String> callback = rest.getForEntity(
                url("/auth/sso/callback?code=abc&redirectUri=http://localhost/callback"), String.class);
        assertThat(callback.getStatusCode().value()).isEqualTo(200);
        assertThat(callback.getBody()).contains("\"token\"");

        User created = userMapper.selectOne(new LambdaQueryWrapper<User>()
                .eq(User::getPhone, "13900000001"));
        assertThat(created).isNotNull();
        assertThat(created.getName()).isEqualTo("模拟用户");
    }

    @Test
    void ssoAuthorizeFailsWhenNotConfigured() {
        ResponseEntity<String> response = rest.getForEntity(
                url("/auth/sso/authorize?redirectUri=http://localhost/callback"), String.class);

        assertThat(response.getStatusCode().value()).isEqualTo(200);
        assertThat(response.getBody()).contains("\"code\":400").contains("统一身份认证未配置");
    }

    private Map<String, Object> login(String phone, String password) {
        Map<String, Object> payload = new HashMap<>();
        payload.put("phone", phone);
        payload.put("password", password);
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        ResponseEntity<Map> response = rest.exchange(url("/auth/login"), HttpMethod.POST,
                new HttpEntity<>(payload, headers), Map.class);
        return response.getBody();
    }

    @SuppressWarnings("unchecked")
    private Map<String, Object> data(Map<String, Object> body) {
        return (Map<String, Object>) body.get("data");
    }

    private String token(Map<String, Object> body) {
        return String.valueOf(data(body).get("token"));
    }

    private ResponseEntity<String> get(String path, String token) {
        return rest.exchange(url(path), HttpMethod.GET, new HttpEntity<>(bearer(token)), String.class);
    }

    private HttpHeaders bearer(String token) {
        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(token);
        return headers;
    }

    private String url(String path) {
        return "http://localhost:" + port + "/api" + path;
    }

    private Long createUser(String phone, String name, int status, RoleEnum role, Long schoolId) {
        User user = new User();
        user.setPhone(phone);
        user.setName(name);
        user.setPassword(passwordEncoder.encode(PASSWORD));
        user.setStatus(status);
        user.setCreatedAt(LocalDateTime.now());
        user.setUpdatedAt(LocalDateTime.now());
        userMapper.insert(user);
        addRole(user.getId(), role, schoolId, 1);
        return user.getId();
    }

    private void addRole(Long userId, RoleEnum role, Long schoolId, int isDefault) {
        UserRole userRole = new UserRole();
        userRole.setUserId(userId);
        userRole.setRole(role);
        userRole.setSchoolId(schoolId);
        userRole.setIsDefault(isDefault);
        userRole.setStatus(1);
        userRole.setCreatedAt(LocalDateTime.now());
        userRole.setUpdatedAt(LocalDateTime.now());
        userRoleMapper.insert(userRole);
    }

    private void insertSsoConfig(String protocol) {
        IntegrationConfig config = new IntegrationConfig();
        config.setIntegrationKey("SSO");
        config.setProtocol(protocol);
        config.setEnabled(1);
        config.setStatus(1);
        config.setTimeoutMs(3000);
        config.setRetryTimes(0);
        config.setCreatedAt(LocalDateTime.now());
        config.setUpdatedAt(LocalDateTime.now());
        integrationConfigMapper.insert(config);
    }
}
