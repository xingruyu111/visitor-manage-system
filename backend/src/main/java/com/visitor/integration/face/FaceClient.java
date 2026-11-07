package com.visitor.integration.face;

/**
 * 人脸平台：人员人脸下发、撤销与核验。
 */
public interface FaceClient {

    boolean isConfigured();

    FaceOpResult enroll(FaceEnrollRequest request);

    FaceOpResult revoke(String personNo);

    FaceVerifyResult verify(String personNo, byte[] image);

    record FaceEnrollRequest(String personNo, String personName, String idCard, String imageUrl) {
    }

    record FaceOpResult(boolean success, String message, String externalRef) {
    }

    record FaceVerifyResult(boolean matched, double score, String message) {
    }
}
