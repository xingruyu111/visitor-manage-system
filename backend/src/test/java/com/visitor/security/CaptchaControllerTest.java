package com.visitor.security;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.test.context.ActiveProfiles;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
class CaptchaControllerTest {

    @LocalServerPort
    private int port;

    @Autowired
    private TestRestTemplate rest;

    @Test
    @SuppressWarnings("unchecked")
    void captchaEndpointIsPublicAndDisabledByDefault() {
        Map<String, Object> body = rest.getForObject(
                "http://localhost:" + port + "/api/auth/captcha", Map.class);

        assertThat(body.get("code")).isEqualTo(200);
        Map<String, Object> data = (Map<String, Object>) body.get("data");
        assertThat(data.get("enabled")).isEqualTo(false);
    }
}
