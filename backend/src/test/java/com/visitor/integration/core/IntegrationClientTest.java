package com.visitor.integration.core;

import com.sun.net.httpserver.HttpServer;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

class IntegrationClientTest {

    private HttpServer server;
    private String baseUrl;
    private final AtomicInteger attempts = new AtomicInteger();
    private IntegrationLogService logService;
    private IntegrationClient client;

    @BeforeEach
    void setUp() throws IOException {
        server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        server.createContext("/ok", exchange -> {
            byte[] body = "{\"ok\":true}".getBytes(StandardCharsets.UTF_8);
            exchange.sendResponseHeaders(200, body.length);
            exchange.getResponseBody().write(body);
            exchange.close();
        });
        server.createContext("/fail", exchange -> {
            attempts.incrementAndGet();
            exchange.sendResponseHeaders(500, -1);
            exchange.close();
        });
        server.createContext("/slow", exchange -> {
            attempts.incrementAndGet();
            try {
                Thread.sleep(1500);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
            exchange.sendResponseHeaders(200, -1);
            exchange.close();
        });
        server.createContext("/echo", exchange -> {
            byte[] request = exchange.getRequestBody().readAllBytes();
            byte[] body = ("{\"len\":" + request.length + "}").getBytes(StandardCharsets.UTF_8);
            exchange.sendResponseHeaders(200, body.length);
            exchange.getResponseBody().write(body);
            exchange.close();
        });
        server.start();
        baseUrl = "http://127.0.0.1:" + server.getAddress().getPort();
        logService = mock(IntegrationLogService.class);
        client = new IntegrationClient(logService);
    }

    @AfterEach
    void tearDown() {
        server.stop(0);
    }

    @Test
    void getSuccessWritesSuccessLog() {
        IntegrationCallResult result = client.get("SSO", baseUrl + "/ok", 2000, 0);

        assertThat(result.success()).isTrue();
        assertThat(result.httpStatus()).isEqualTo(200);
        assertThat(result.body()).contains("\"ok\"");
        verify(logService).record(any());
    }

    @Test
    void retriesOnServerErrorAndRecordsFailure() {
        IntegrationCallResult result = client.get("GATE", baseUrl + "/fail", 2000, 2);

        assertThat(result.success()).isFalse();
        assertThat(result.httpStatus()).isEqualTo(500);
        assertThat(attempts.get()).isEqualTo(3);
        assertThat(result.error()).isNotBlank();
        verify(logService).record(any());
    }

    @Test
    void timeoutIsTreatedAsFailure() {
        long start = System.currentTimeMillis();
        IntegrationCallResult result = client.get("FACE", baseUrl + "/slow", 300, 0);
        long elapsed = System.currentTimeMillis() - start;

        assertThat(result.success()).isFalse();
        assertThat(result.error()).isNotBlank();
        assertThat(elapsed).isLessThan(1500);
    }

    @Test
    void postSendsJsonBody() {
        IntegrationCallResult result = client.post("TODO_MSG", baseUrl + "/echo", Map.of("name", "visitor"), 2000, 0);

        assertThat(result.success()).isTrue();
        assertThat(result.body()).contains("\"len\"");
        assertThat(result.body()).doesNotContain("\"len\":0");
    }
}
