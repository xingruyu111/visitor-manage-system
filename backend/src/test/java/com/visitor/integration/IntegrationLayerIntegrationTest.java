package com.visitor.integration;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.visitor.common.util.SecretCipher;
import com.visitor.domain.entity.IntegrationConfig;
import com.visitor.integration.core.IntegrationNotConfiguredException;
import com.visitor.server.mapper.IntegrationConfigMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpMethod;
import org.springframework.test.context.ActiveProfiles;

import java.time.LocalDateTime;
import java.util.HashMap;
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

    @BeforeEach
    void setUp() {
        configMapper.delete(null);
        insert("SSO", 0, secretCipher.encrypt("sso-secret"));
        insert("GATE", 0, null);
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
    void unknownKeyReturns400() {
        String body = get("/integration-configs/UNKNOWN");
        assertThat(body).contains("\"code\":400");
    }

    private String get(String path) {
        return rest.getForObject(baseUrl() + path, String.class);
    }

    private String put(String path, Map<String, ?> payload) {
        Map<String, Object> body = new HashMap<>(payload);
        return rest.exchange(baseUrl() + path, HttpMethod.PUT, new HttpEntity<>(body), String.class).getBody();
    }

    private String post(String path) {
        return rest.postForObject(baseUrl() + path, null, String.class);
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
