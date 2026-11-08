package com.visitor.integration.face;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * 人脸平台 Mock：下发/撤销/核验均返回成功，用于联调演示。
 */
@Slf4j
@Component
public class MockFaceClient implements FaceClient {

    private final List<String> enrolled = new CopyOnWriteArrayList<>();
    private final List<String> revoked = new CopyOnWriteArrayList<>();
    private final List<String> verified = new CopyOnWriteArrayList<>();

    @Override
    public String protocol() {
        return "MOCK";
    }

    @Override
    public boolean isConfigured() {
        return true;
    }

    @Override
    public FaceOpResult enroll(FaceEnrollRequest request) {
        enrolled.add(request.personNo());
        log.info("[MOCK-人脸] 下发人脸: {}", request.personNo());
        return new FaceOpResult(true, "OK", "mock-face-" + request.personNo());
    }

    @Override
    public FaceOpResult revoke(String personNo) {
        revoked.add(personNo);
        return new FaceOpResult(true, "OK", "mock-revoke-" + personNo);
    }

    @Override
    public FaceVerifyResult verify(String personNo, byte[] image) {
        verified.add(personNo);
        return new FaceVerifyResult(true, 0.99, "OK");
    }

    public List<String> enrolled() {
        return enrolled;
    }

    public List<String> revoked() {
        return revoked;
    }

    public List<String> verified() {
        return verified;
    }

    public void clear() {
        enrolled.clear();
        revoked.clear();
        verified.clear();
    }
}
