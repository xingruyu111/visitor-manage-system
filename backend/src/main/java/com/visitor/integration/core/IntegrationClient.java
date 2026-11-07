package com.visitor.integration.core;

import com.visitor.domain.entity.IntegrationLog;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientResponseException;

/**
 * 外部调用统一入口：超时、重试、耗时与调用日志。
 *
 * <p>M1 只提供基础能力，具体签名（HMAC 等）在各 Port 的真实实现中扩展。</p>
 */
@Component
public class IntegrationClient {

    private static final Logger log = LoggerFactory.getLogger(IntegrationClient.class);
    private static final int MAX_RETRY_SLEEP_MS = 500;

    private final IntegrationLogService logService;

    public IntegrationClient(IntegrationLogService logService) {
        this.logService = logService;
    }

    public IntegrationCallResult get(String integrationKey, String url, int timeoutMs, int retryTimes) {
        return execute(integrationKey, "GET", url, null, timeoutMs, retryTimes);
    }

    public IntegrationCallResult post(String integrationKey, String url, Object body, int timeoutMs, int retryTimes) {
        return execute(integrationKey, "POST", url, body, timeoutMs, retryTimes);
    }

    private IntegrationCallResult execute(String integrationKey, String method, String url,
                                          Object body, int timeoutMs, int retryTimes) {
        long start = System.currentTimeMillis();
        int attempts = Math.max(retryTimes, 0) + 1;
        int lastStatus = 0;
        String lastError = null;

        for (int attempt = 1; attempt <= attempts; attempt++) {
            try {
                RestClient client = RestClient.builder()
                        .requestFactory(requestFactory(timeoutMs))
                        .build();

                ResponseEntity<String> response = "POST".equals(method)
                        ? client.post().uri(url).contentType(MediaType.APPLICATION_JSON)
                            .body(body == null ? "{}" : body).retrieve().toEntity(String.class)
                        : client.get().uri(url).retrieve().toEntity(String.class);

                long cost = System.currentTimeMillis() - start;
                record(integrationKey, method, url, response.getStatusCode().value(), true, null, (int) cost);
                return IntegrationCallResult.ok(response.getStatusCode().value(), response.getBody(), cost);
            } catch (RestClientResponseException e) {
                lastStatus = e.getStatusCode().value();
                lastError = e.getStatusText() + " " + truncate(e.getResponseBodyAsString());
            } catch (Exception e) {
                lastError = e.getClass().getSimpleName() + ": " + e.getMessage();
            }
            if (attempt < attempts) {
                sleep(MAX_RETRY_SLEEP_MS);
            }
        }

        long cost = System.currentTimeMillis() - start;
        log.warn("集成调用失败: key={}, method={}, url={}, error={}", integrationKey, method, url, lastError);
        record(integrationKey, method, url, lastStatus, false, lastError, (int) cost);
        return IntegrationCallResult.fail(lastStatus, lastError, cost);
    }

    private void record(String integrationKey, String method, String url,
                        int status, boolean success, String error, int costMs) {
        IntegrationLog entry = new IntegrationLog();
        entry.setIntegrationKey(integrationKey);
        entry.setAction(method + " " + actionPath(url));
        entry.setDirection("OUT");
        entry.setRequestDigest(truncate(method + " " + url));
        entry.setHttpStatus(status == 0 ? null : status);
        entry.setSuccess(success ? 1 : 0);
        entry.setErrorMsg(error == null ? null : truncate(error));
        entry.setCostMs(costMs);
        logService.record(entry);
    }

    private String actionPath(String url) {
        try {
            return java.net.URI.create(url).getPath();
        } catch (Exception e) {
            return url;
        }
    }

    private String truncate(String value) {
        if (value == null) {
            return null;
        }
        return value.length() <= 512 ? value : value.substring(0, 512);
    }

    private SimpleClientHttpRequestFactory requestFactory(int timeoutMs) {
        int timeout = Math.max(timeoutMs, 1000);
        SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
        factory.setConnectTimeout(timeout);
        factory.setReadTimeout(timeout);
        return factory;
    }

    private void sleep(long millis) {
        try {
            Thread.sleep(millis);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }
}
