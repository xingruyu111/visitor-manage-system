package com.visitor.integration.face;

import com.visitor.integration.core.IntegrationNotConfiguredException;

/**
 * 未配置人脸平台时的兜底实现：隐藏人脸入口，只保留扫码/手动核验。
 */
public final class NoopFaceClient implements FaceClient {

    public static final NoopFaceClient INSTANCE = new NoopFaceClient();
    private static final String KEY = "FACE";

    private NoopFaceClient() {
    }

    @Override
    public boolean isConfigured() {
        return false;
    }

    @Override
    public FaceOpResult enroll(FaceEnrollRequest request) {
        throw new IntegrationNotConfiguredException(KEY);
    }

    @Override
    public FaceOpResult revoke(String personNo) {
        throw new IntegrationNotConfiguredException(KEY);
    }

    @Override
    public FaceVerifyResult verify(String personNo, byte[] image) {
        throw new IntegrationNotConfiguredException(KEY);
    }
}
