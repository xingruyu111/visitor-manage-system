package com.visitor.integration.sso;

import com.visitor.integration.core.IntegrationNotConfiguredException;

/**
 * 未配置 SSO 时的兜底实现：业务侧应回落本地账号登录。
 */
public final class NoopIdentityProvider implements IdentityProvider {

    public static final NoopIdentityProvider INSTANCE = new NoopIdentityProvider();
    private static final String KEY = "SSO";

    private NoopIdentityProvider() {
    }

    @Override
    public String protocol() {
        return "NONE";
    }

    @Override
    public boolean isConfigured() {
        return false;
    }

    @Override
    public String buildAuthorizeUrl(String redirectUri, String state) {
        throw new IntegrationNotConfiguredException(KEY);
    }

    @Override
    public SsoUser exchange(String code, String redirectUri) {
        throw new IntegrationNotConfiguredException(KEY);
    }
}
