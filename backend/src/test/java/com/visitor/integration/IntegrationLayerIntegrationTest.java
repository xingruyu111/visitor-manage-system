package com.visitor.integration;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.visitor.common.enums.RoleEnum;
import com.visitor.common.util.SecretCipher;
import com.visitor.domain.entity.IntegrationConfig;
import com.visitor.domain.entity.IntegrationLog;
import com.visitor.domain.entity.User;
import com.visitor.domain.entity.UserRole;
import com.visitor.integration.core.IntegrationLogService;
import com.visitor.integration.core.IntegrationNotConfiguredException;
import com.visitor.security.JwtUtil;
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
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
class IntegrationLayerIntegrationTest {

    @LocalServerPort
    private int port;

    @Autowired
    private TestRestTemplate rest;

    @Autowired
    private IntegrationConfigMapper configMapper;

    @Autowired
    private SecretCipher secretCipher;

    @Autowired
    private IntegrationFactory integrationFactory;

    @Autowired
    private IntegrationLogService logService;

    @Autowired
    private UserMapper userMapper;

    @Autowired
    private UserRoleMapper userRoleMapper;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private JwtUtil jwtUtil;

    private String token;

    @BeforeEach
    void setUp() {
        configMapper.delete(null);
        insert("SSO", 0, secretCipher.encrypt("sso-secret"));
        insert("GATE", 0, null);
        token = superAdminToken();
    }

    @Test
    void listReturnsMaskedSecret() {
        String body = get("/integration-configs");
        assertThat(body).contains("******");
        assertThat(body).doesNotContain("sso-secret");
    }

    @Test
    void saveWithBlankSecretKeepsExistingCipherText() {
        String body = put("/integration-configs/SSO", Map.of("baseUrl", "http://127.0.0.1:9/mock", "appSecret", ""));
        assertThat(body).contains("\"code\":200");

        IntegrationConfig saved = findByKey("SSO");
        assertThat(saved.getAppSecret()).startsWith("enc:v1:");
        assertThat(secretCipher.decrypt(saved.getAppSecret())).isEqualTo("sso-secret");
        assertThat(saved.getBaseUrl()).isEqualTo("http://127.0.0.1:9/mock");
    }

    @Test
    void saveEncryptsNewSecret() {
        put("/integration-configs/SSO", Map.of("appSecret", "new-secret"));

        IntegrationConfig saved = findByKey("SSO");
        assertThat(saved.getAppSecret()).startsWith("enc:v1:");
        assertThat(secretCipher.decrypt(saved.getAppSecret())).isEqualTo("new-secret");
    }

    @Test
    void disabledConfigFallsBackToNoop() {
        assertThat(integrationFactory.identity().isConfigured()).isFalse();
        assertThatThrownBy(() -> integrationFactory.identity().buildAuthorizeUrl("r", "s"))
                .isInstanceOf(IntegrationNotConfiguredException.class);
        assertThatThrownBy(() -> integrationFactory.gate().pushPermission(null))
                .isInstanceOf(IntegrationNotConfiguredException.class);
    }

    @Test
    void testEndpointReportsNotEnabled() {
        String body = post("/integration-configs/SSO/test");
        assertThat(body).contains("\"configured\":false");
        assertThat(body).contains("未启用");
    }

    @Test
    void saveRejectsInvalidJson() {
        String body = put("/integration-configs/SSO", Map.of("endpoints", "not-json"));

        assertThat(body).contains("\"code\":400");
        assertThat(body).contains("不是合法 JSON");
    }

    @Test
    void saveRejectsOutOfRangeTimeout() {
        String body = put("/integration-configs/SSO", Map.of("timeoutMs", 50));

        assertThat(body).contains("\"code\":400");
    }

    @Test
    void maskedSecretDoesNotOverwriteStoredSecret() {
        put("/integration-configs/SSO", Map.of("appSecret", "******"));

        IntegrationConfig saved = findByKey("SSO");
        assertThat(secretCipher.decrypt(saved.getAppSecret())).isEqualTo("sso-secret");
    }

    @Test
    void logsEndpointReturnsPagedResult() {
        IntegrationLog entry = new IntegrationLog();
        entry.setIntegrationKey("SSO");
        entry.setAction("GET /oauth2/userinfo");
        entry.setSuccess(1);
        logService.record(entry);

        String body = get("/integration-logs?integrationKey=SSO&page=1&size=10");

        assertThat(body).contains("\"total\":1");
        assertThat(body).contains("GET /oauth2/userinfo");
    }

    @Test
    void unknownKeyReturns400() {
        String body = get("/integration-configs/UNKNOWN");
        assertThat(body).contains("\"code\":400");
    }

    private String get(String path) {
        return rest.exchange(baseUrl() + path, HttpMethod.GET, new HttpEntity<>(bearer()), String.class).getBody();
    }

    private String put(String path, Map<String, ?> payload) {
        Map<String, Object> body = new HashMap<>(payload);
        HttpHeaders headers = bearer();
        headers.setContentType(MediaType.APPLICATION_JSON);
        return rest.exchange(baseUrl() + path, HttpMethod.PUT,
                new HttpEntity<>(body, headers), String.class).getBody();
    }

    private String post(String path) {
        return rest.exchange(baseUrl() + path, HttpMethod.POST,
                new HttpEntity<>(bearer()), String.class).getBody();
    }

    private HttpHeaders bearer() {
        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(token);
        return headers;
    }

    private String superAdminToken() {
        userRoleMapper.delete(null);
        userMapper.delete(null);

        User user = new User();
        user.setPhone("13700000001");
        user.setName("集成测试管理员");
        user.setPassword(passwordEncoder.encode("pass1234"));
        user.setStatus(1);
        user.setCreatedAt(LocalDateTime.now());
        user.setUpdatedAt(LocalDateTime.now());
        userMapper.insert(user);

        UserRole role = new UserRole();
        role.setUserId(user.getId());
        role.setRole(RoleEnum.SYSTEM_ADMIN);
        role.setIsDefault(1);
        role.setStatus(1);
        role.setCreatedAt(LocalDateTime.now());
        role.setUpdatedAt(LocalDateTime.now());
        userRoleMapper.insert(role);

        return jwtUtil.generate(user.getId(), user.getName(),
                List.of(RoleEnum.SYSTEM_ADMIN), RoleEnum.SYSTEM_ADMIN, null);
    }

    private String baseUrl() {
        return "http://localhost:" + port + "/api/web/websuper";
    }

    private void insert(String key, int enabled, String secret) {
        IntegrationConfig config = new IntegrationConfig();
        config.setIntegrationKey(key);
        config.setEnabled(enabled);
        config.setStatus(1);
        config.setTimeoutMs(3000);
        config.setRetryTimes(1);
        config.setAppSecret(secret);
        config.setCreatedAt(LocalDateTime.now());
        config.setUpdatedAt(LocalDateTime.now());
        configMapper.insert(config);
    }

    private IntegrationConfig findByKey(String key) {
        return configMapper.selectOne(new LambdaQueryWrapper<IntegrationConfig>()
                .eq(IntegrationConfig::getIntegrationKey, key));
    }
}
