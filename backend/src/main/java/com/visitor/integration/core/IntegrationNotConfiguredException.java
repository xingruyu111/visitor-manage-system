package com.visitor.integration.core;

/**
 * 接口未配置时抛出，调用方据此降级而不是中断业务。
 */
public class IntegrationNotConfiguredException extends RuntimeException {

    private final String integrationKey;

    public IntegrationNotConfiguredException(String integrationKey) {
        super("集成项未配置: " + integrationKey);
        this.integrationKey = integrationKey;
    }

    public String getIntegrationKey() {
        return integrationKey;
    }
}
