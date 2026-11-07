package com.visitor.integration;

import com.visitor.domain.entity.IntegrationConfig;
import com.visitor.integration.sso.IdentityProvider;
import com.visitor.server.mapper.IntegrationConfigMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;

import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@ActiveProfiles("test")
@Import(IntegrationFactoryTest.StubConfig.class)
class IntegrationFactoryTest {

    @Autowired
    private IntegrationConfigMapper configMapper;

    @Autowired
    private IntegrationFactory factory;

    @BeforeEach
    void setUp() {
        configMapper.delete(null);
    }

    @Test
    void picksImplementationMatchingConfiguredProtocol() {
        insert("SSO", 1, "STUB");

        IdentityProvider provider = factory.identity();

        assertThat(provider.isConfigured()).isTrue();
        assertThat(provider).isInstanceOf(StubIdentityProvider.class);
        assertThat(provider.buildAuthorizeUrl("https://app/callback", "state")).contains("stub");
    }

    @Test
    void fallsBackToNoopWhenDisabled() {
        insert("SSO", 0, "STUB");

        assertThat(factory.identity().isConfigured()).isFalse();
    }

    @Test
    void fallsBackToNoopWhenProtocolHasNoImplementation() {
        insert("SSO", 1, "CAS");

        assertThat(factory.identity().isConfigured()).isFalse();
    }

    @Test
    void fallsBackToNoopWhenProtocolIsBlank() {
        insert("SSO", 1, null);

        assertThat(factory.identity().isConfigured()).isFalse();
    }

    @Test
    void otherPortsFallBackWhenNotConfigured() {
        assertThat(factory.dataSync().isConfigured()).isFalse();
        assertThat(factory.gate().isConfigured()).isFalse();
        assertThat(factory.face().isConfigured()).isFalse();
        assertThat(factory.portal().isConfigured()).isFalse();
        assertThat(factory.todoMessage().isConfigured()).isFalse();
    }

    private void insert(String key, int enabled, String protocol) {
        IntegrationConfig config = new IntegrationConfig();
        config.setIntegrationKey(key);
        config.setEnabled(enabled);
        config.setStatus(1);
        config.setProtocol(protocol);
        config.setTimeoutMs(3000);
        config.setRetryTimes(1);
        config.setCreatedAt(LocalDateTime.now());
        config.setUpdatedAt(LocalDateTime.now());
        configMapper.insert(config);
    }

    @TestConfiguration
    static class StubConfig {

        @Bean
        IdentityProvider stubIdentityProvider() {
            return new StubIdentityProvider();
        }
    }

    static class StubIdentityProvider implements IdentityProvider {

        @Override
        public String protocol() {
            return "STUB";
        }

        @Override
        public boolean isConfigured() {
            return true;
        }

        @Override
        public String buildAuthorizeUrl(String redirectUri, String state) {
            return "https://stub.example.com/authorize?redirect=" + redirectUri + "&state=" + state;
        }

        @Override
        public SsoUser exchange(String code, String redirectUri) {
            return new SsoUser("ext-1", "stub", "Stub User", null, null, null, "TEACHER", java.util.Map.of());
        }
    }
}
