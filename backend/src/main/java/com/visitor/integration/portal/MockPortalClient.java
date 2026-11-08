package com.visitor.integration.portal;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * 融合门户 / i 阳光 Mock：用于演示单点跳转与票据校验。
 */
@Slf4j
@Component
public class MockPortalClient implements PortalClient {

    private final List<String> calls = new CopyOnWriteArrayList<>();

    @Override
    public String protocol() {
        return "MOCK";
    }

    @Override
    public boolean isConfigured() {
        return true;
    }

    @Override
    public String buildSsoUrl(String targetPath) {
        calls.add("sso:" + targetPath);
        log.info("[MOCK-门户] 生成跳转地址: {}", targetPath);
        return "mock://portal/sso?target=" + targetPath;
    }

    @Override
    public PortalUser validateTicket(String ticket) {
        calls.add("ticket:" + ticket);
        log.info("[MOCK-门户] 校验票据: {}", ticket);
        return new PortalUser("ext-" + ticket, "mock-" + ticket, "门户用户", "13900000000", "TEACHER");
    }

    public List<String> calls() {
        return calls;
    }

    public void clear() {
        calls.clear();
    }
}
