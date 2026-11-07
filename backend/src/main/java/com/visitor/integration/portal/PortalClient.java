package com.visitor.integration.portal;

/**
 * 融合门户 / i 阳光：单点跳转与票据校验。
 */
public interface PortalClient {

    boolean isConfigured();

    String buildSsoUrl(String targetPath);

    PortalUser validateTicket(String ticket);

    record PortalUser(String externalId, String loginName, String name, String phone, String personType) {
    }
}
