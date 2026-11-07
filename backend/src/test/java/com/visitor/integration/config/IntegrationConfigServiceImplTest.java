package com.visitor.integration.config;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.visitor.common.util.SecretCipher;
import com.visitor.domain.dto.IntegrationConfigSaveDTO;
import com.visitor.domain.entity.IntegrationConfig;
import com.visitor.domain.vo.IntegrationConfigVO;
import com.visitor.server.mapper.IntegrationConfigMapper;
import com.visitor.server.mapper.IntegrationLogMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import java.time.LocalDateTime;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@ActiveProfiles("test")
class IntegrationConfigServiceImplTest {

    @Autowired
    private IntegrationConfigService configService;

    @Autowired
    private IntegrationConfigMapper configMapper;

    @Autowired
    private IntegrationLogMapper logMapper;

    @Autowired
    private SecretCipher secretCipher;

    @BeforeEach
    void setUp() {
        configMapper.delete(null);
        logMapper.delete(null);
        insert("SSO", 0, null);
    }

    @Test
    void saveUpdatesAllEditableFields() {
        IntegrationConfigSaveDTO dto = new IntegrationConfigSaveDTO();
        dto.setProtocol("OIDC");
        dto.setEnabled(true);
        dto.setBaseUrl("https://sso.example.edu.cn");
        dto.setAuthType("OAUTH2");
        dto.setAppId("visitor-h5");
        dto.setAppSecret("sso-secret");
        dto.setEndpoints("{\"authorize\":\"/oauth2/authorize\"}");
        dto.setFieldMapping("{\"name\":\"cn\"}");
        dto.setExtra("{\"tenant\":\"main\"}");
        dto.setTimeoutMs(3000);
        dto.setRetryTimes(2);
        dto.setStatus(1);
        dto.setRemark("统一身份");

        IntegrationConfigVO vo = configService.save("SSO", dto);

        assertThat(vo.getProtocol()).isEqualTo("OIDC");
        assertThat(vo.getEnabled()).isEqualTo(1);
        assertThat(vo.getAppSecret()).isEqualTo("******");
        assertThat(vo.getSecretConfigured()).isTrue();
        assertThat(vo.getEndpoints()).contains("authorize");

        IntegrationConfig saved = findByKey("SSO");
        assertThat(secretCipher.decrypt(saved.getAppSecret())).isEqualTo("sso-secret");
        assertThat(saved.getRemark()).isEqualTo("统一身份");
    }

    @Test
    void testReportsMissingBaseUrl() {
        enable("SSO", null);

        Map<String, Object> result = configService.test("SSO");

        assertThat(result.get("configured")).isEqualTo(false);
        assertThat(result.get("message")).isEqualTo("未配置地址");
    }

    @Test
    void testReportsUnreachableAndWritesLog() {
        enable("SSO", "http://127.0.0.1:1/health");

        Map<String, Object> result = configService.test("SSO");

        assertThat(result.get("configured")).isEqualTo(true);
        assertThat(result.get("reachable")).isEqualTo(false);
        assertThat(result.get("error")).isNotNull();
        assertThat(logMapper.selectCount(null)).isGreaterThan(0);
    }

    private void enable(String key, String baseUrl) {
        IntegrationConfig config = findByKey(key);
        config.setEnabled(1);
        config.setBaseUrl(baseUrl);
        config.setTimeoutMs(800);
        config.setRetryTimes(0);
        config.setUpdatedAt(LocalDateTime.now());
        configMapper.updateById(config);
    }

    private void insert(String key, int enabled, String protocol) {
        IntegrationConfig config = new IntegrationConfig();
        config.setIntegrationKey(key);
        config.setEnabled(enabled);
        config.setProtocol(protocol);
        config.setStatus(1);
        config.setTimeoutMs(800);
        config.setRetryTimes(0);
        config.setCreatedAt(LocalDateTime.now());
        config.setUpdatedAt(LocalDateTime.now());
        configMapper.insert(config);
    }

    private IntegrationConfig findByKey(String key) {
        return configMapper.selectOne(new LambdaQueryWrapper<IntegrationConfig>()
                .eq(IntegrationConfig::getIntegrationKey, key));
    }
}
