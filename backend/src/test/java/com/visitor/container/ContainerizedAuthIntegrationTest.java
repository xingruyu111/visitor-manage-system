package com.visitor.container;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.MySQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.DockerImageName;

import java.util.HashMap;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * 真实 MySQL + Redis 的容器化测试。
 *
 * <p>没有 Docker 的环境（如本机）自动跳过；CI 或装有 Docker 的服务器上会执行。</p>
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@Testcontainers(disabledWithoutDocker = true)
class ContainerizedAuthIntegrationTest {

    @Container
    static final MySQLContainer<?> MYSQL = new MySQLContainer<>(DockerImageName.parse("mysql:8.0"))
            .withDatabaseName("visitor_test")
            .withUsername("visitor")
            .withPassword("visitor")
            .withCommand("--innodb-buffer-pool-size=32M", "--performance-schema=OFF", "--max-connections=50");

    @Container
    static final GenericContainer<?> REDIS =
            new GenericContainer<>(DockerImageName.parse("redis:7-alpine"))
                    .withExposedPorts(6379)
                    .withCommand("redis-server", "--save", "", "--appendonly", "no");

    @DynamicPropertySource
    static void registerProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", MYSQL::getJdbcUrl);
        registry.add("spring.datasource.username", MYSQL::getUsername);
        registry.add("spring.datasource.password", MYSQL::getPassword);
        registry.add("spring.data.redis.host", REDIS::getHost);
        registry.add("spring.data.redis.port", () -> REDIS.getMappedPort(6379));
        registry.add("spring.sql.init.mode", () -> "always");
        registry.add("spring.sql.init.schema-locations", () -> "classpath:sql/schema-mysql.sql");
    }

    @LocalServerPort
    private int port;

    @Autowired
    private TestRestTemplate rest;

    @Test
    void loginWorksAgainstRealMysql() {
        Map<String, Object> body = login("13800000000", "admin123");

        assertThat(body.get("code")).isEqualTo(200);
        assertThat(data(body).get("token")).asString().isNotBlank();
    }

    @Test
    void logoutInvalidatesTokenAgainstRealRedis() {
        String token = token(login("13800000000", "admin123"));

        rest.exchange(url("/auth/logout"), HttpMethod.POST,
                new HttpEntity<>(bearer(token)), String.class);
        ResponseEntity<String> replay = rest.exchange(url("/auth/me"), HttpMethod.GET,
                new HttpEntity<>(bearer(token)), String.class);

        assertThat(replay.getStatusCode().value()).isEqualTo(401);
    }

    @Test
    void fiveFailuresLockThePhoneAgainstRealRedis() {
        for (int i = 0; i < 5; i++) {
            assertThat(login("13900009999", "wrong").get("code")).isEqualTo(401);
        }

        Map<String, Object> sixth = login("13900009999", "wrong");

        assertThat(sixth.get("code")).isEqualTo(429);
        assertThat(sixth.get("message")).asString().contains("尝试过于频繁");
    }

    @Test
    void lockIsPerPhoneAndDoesNotAffectOthers() {
        for (int i = 0; i < 5; i++) {
            login("13900008888", "wrong");
        }

        assertThat(login("13800000000", "admin123").get("code")).isEqualTo(200);
    }

    private Map<String, Object> login(String phone, String password) {
        Map<String, Object> payload = new HashMap<>();
        payload.put("phone", phone);
        payload.put("password", password);
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        ResponseEntity<Map> response = rest.exchange(url("/auth/login"), HttpMethod.POST,
                new HttpEntity<>(payload, headers), Map.class);
        return response.getBody();
    }

    @SuppressWarnings("unchecked")
    private Map<String, Object> data(Map<String, Object> body) {
        return (Map<String, Object>) body.get("data");
    }

    private String token(Map<String, Object> body) {
        return String.valueOf(data(body).get("token"));
    }

    private HttpHeaders bearer(String token) {
        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(token);
        return headers;
    }

    private String url(String path) {
        return "http://localhost:" + port + "/api" + path;
    }
}
