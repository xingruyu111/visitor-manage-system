package com.visitor.integration;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.visitor.domain.entity.IntegrationConfig;
import com.visitor.integration.face.FaceClient;
import com.visitor.integration.face.MockFaceClient;
import com.visitor.integration.portal.MockPortalClient;
import com.visitor.integration.portal.PortalClient;
import com.visitor.server.mapper.IntegrationConfigMapper;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@ActiveProfiles("test")
class MockPortalFaceTest {

    @Autowired
    private IntegrationFactory factory;

    @Autowired
    private IntegrationConfigMapper configMapper;

    @Autowired
    private MockPortalClient mockPortalClient;

    @Autowired
    private MockFaceClient mockFaceClient;

    @BeforeEach
    void setUp() {
        configMapper.delete(new LambdaQueryWrapper<IntegrationConfig>()
                .in(IntegrationConfig::getIntegrationKey, "PORTAL", "FACE"));
        insert("PORTAL");
        insert("FACE");
        mockPortalClient.clear();
        mockFaceClient.clear();
    }

    @AfterEach
    void tearDown() {
        configMapper.delete(new LambdaQueryWrapper<IntegrationConfig>()
                .in(IntegrationConfig::getIntegrationKey, "PORTAL", "FACE"));
    }

    @Test
    void portalMockBuildsSsoAndValidatesTicket() {
        PortalClient portal = factory.portal();
        assertThat(portal).isInstanceOf(MockPortalClient.class);

        assertThat(portal.buildSsoUrl("/visitor/apply")).contains("mock://portal");
        PortalClient.PortalUser user = portal.validateTicket("t-1");
        assertThat(user.name()).isEqualTo("门户用户");
        assertThat(mockPortalClient.calls()).hasSize(2);
    }

    @Test
    void faceMockSupportsEnrollVerifyAndRevoke() {
        FaceClient face = factory.face();
        assertThat(face).isInstanceOf(MockFaceClient.class);

        assertThat(face.enroll(new FaceClient.FaceEnrollRequest("P1", "张三", null, null)).success()).isTrue();
        assertThat(face.verify("P1", new byte[]{1, 2}).matched()).isTrue();
        assertThat(face.revoke("P1").success()).isTrue();
        assertThat(mockFaceClient.enrolled()).containsExactly("P1");
        assertThat(mockFaceClient.verified()).containsExactly("P1");
        assertThat(mockFaceClient.revoked()).containsExactly("P1");
    }

    private void insert(String key) {
        IntegrationConfig config = new IntegrationConfig();
        config.setIntegrationKey(key);
        config.setProtocol("MOCK");
        config.setEnabled(1);
        config.setStatus(1);
        config.setCreatedAt(LocalDateTime.now());
        config.setUpdatedAt(LocalDateTime.now());
        configMapper.insert(config);
    }
}
