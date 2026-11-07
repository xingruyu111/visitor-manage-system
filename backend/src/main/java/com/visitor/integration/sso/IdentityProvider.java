package com.visitor.integration.sso;

import java.util.Map;

/**
 * 统一身份认证：CAS / OIDC / OAuth2 由实现类的 protocol() 区分。
 */
public interface IdentityProvider {

    String protocol();

    boolean isConfigured();

    String buildAuthorizeUrl(String redirectUri, String state);

    SsoUser exchange(String code, String redirectUri);

    record SsoUser(String externalId, String loginName, String name, String phone,
                   String email, String deptCode, String personType, Map<String, Object> raw) {
    }
}
