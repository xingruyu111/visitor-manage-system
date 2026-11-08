package com.visitor.wechat;

import com.visitor.domain.entity.MpConfig;
import com.visitor.server.mapper.MpConfigMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;

import java.time.LocalDateTime;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
class WechatPhoneTest {

    @LocalServerPort
    private int port;

    @Autowired
    private TestRestTemplate rest;

    @Autowired
    private MpConfigMapper mpConfigMapper;

    @BeforeEach
    void setUp() {
        mpConfigMapper.delete(null);
    }

    @Test
    void requiresConfiguration() {
        Map<String, Object> body = post("abc");
        assertThat(body.get("code")).isEqualTo(400);
    }

    @Test
    @SuppressWarnings("unchecked")
    void mockReturnsStablePhone() {
        insertConfig("MOCK");

        Map<String, Object> first = post("phone-code-1");
        Map<String, Object> data = (Map<String, Object>) first.get("data");
        String phone = data.get("phone").toString();
        assertThat(phone).hasSize(11).startsWith("139");

        Map<String, Object> second = post("phone-code-1");
        assertThat(((Map<String, Object>) second.get("data")).get("phone")).isEqualTo(phone);
    }

    @Test
    void restModeNeedsMiniProgramCredentials() {
        insertConfig("REST");

        Map<String, Object> body = post("abc");
        assertThat(body.get("code")).isEqualTo(400);
        assertThat(body.get("message").toString()).contains("小程序凭证");
    }

    private Map<String, Object> post(String phoneCode) {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        return rest.postForObject("http://localhost:" + port + "/api/auth/wechat/phone",
                new HttpEntity<>(Map.of("phoneCode", phoneCode), headers), Map.class);
    }

    private void insertConfig(String mode) {
        MpConfig config = new MpConfig();
        config.setAppId("wx-test");
        config.setSecret("secret");
        config.setAuthMode(mode);
        config.setCreatedAt(LocalDateTime.now());
        config.setUpdatedAt(LocalDateTime.now());
        mpConfigMapper.insert(config);
    }
}
