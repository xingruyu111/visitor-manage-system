package com.visitor.integration.sso;

import org.springframework.stereotype.Component;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.Map;

/**
 * 联调用 Mock 统一认证：把 vst_integration_config 中 SSO 的 protocol 设为 MOCK 即可启用。
 */
@Component
public class MockIdentityProvider implements IdentityProvider {

    @Override
    public String protocol() {
        return "MOCK";
    }

    @Override
    public boolean isConfigured() {
        return true;
    }

    @Override
    public String buildAuthorizeUrl(String redirectUri, String state) {
        return "mock://sso/authorize?redirect_uri="
                + URLEncoder.encode(redirectUri, StandardCharsets.UTF_8)
                + "&state=" + URLEncoder.encode(state == null ? "" : state, StandardCharsets.UTF_8);
    }

    @Override
    public SsoUser exchange(String code, String redirectUri) {
        return new SsoUser("mock-" + code, "mock-user", "模拟用户",
                "13900000001", "mock@example.edu.cn", "D001", "TEACHER", Map.of());
    }
}
