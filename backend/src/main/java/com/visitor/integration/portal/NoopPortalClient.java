package com.visitor.integration.portal;

import com.visitor.integration.core.IntegrationNotConfiguredException;

/**
 * 未配置门户时的兜底实现：H5 使用自带登录，不展示门户入口。
 */
public final class NoopPortalClient implements PortalClient {

    public static final NoopPortalClient INSTANCE = new NoopPortalClient();
    private static final String KEY = "PORTAL";

    private NoopPortalClient() {
    }

    @Override
    public boolean isConfigured() {
        return false;
    }

    @Override
    public String buildSsoUrl(String targetPath) {
        throw new IntegrationNotConfiguredException(KEY);
    }

    @Override
    public PortalUser validateTicket(String ticket) {
        throw new IntegrationNotConfiguredException(KEY);
    }
}
