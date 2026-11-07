package com.visitor.integration.core;

/**
 * 外部调用结果。
 */
public record IntegrationCallResult(boolean success, int httpStatus, String body, long costMs, String error) {

    public static IntegrationCallResult ok(int status, String body, long costMs) {
        return new IntegrationCallResult(true, status, body, costMs, null);
    }

    public static IntegrationCallResult fail(int status, String error, long costMs) {
        return new IntegrationCallResult(false, status, null, costMs, error);
    }
}
