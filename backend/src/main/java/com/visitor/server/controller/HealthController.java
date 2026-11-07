package com.visitor.server.controller;

import com.visitor.common.core.Result;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.data.redis.connection.RedisConnection;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * 健康检查：同时探测数据库与 Redis，便于部署探活和本地联调。
 */
@Tag(name = "健康检查")
@RestController
@RequestMapping("/health")
public class HealthController {

    private final JdbcTemplate jdbcTemplate;
    private final StringRedisTemplate redisTemplate;

    public HealthController(JdbcTemplate jdbcTemplate, StringRedisTemplate redisTemplate) {
        this.jdbcTemplate = jdbcTemplate;
        this.redisTemplate = redisTemplate;
    }

    @Operation(summary = "服务健康检查（含 MySQL 与 Redis）")
    @GetMapping
    public Result<Map<String, Object>> health() {
        Map<String, Object> database = checkDatabase();
        Map<String, Object> redis = checkRedis();

        boolean up = "UP".equals(database.get("status")) && "UP".equals(redis.get("status"));

        Map<String, Object> body = new LinkedHashMap<>();
        body.put("status", up ? "UP" : "DEGRADED");
        body.put("service", "visitor-manage-system");
        body.put("database", database);
        body.put("redis", redis);
        body.put("time", LocalDateTime.now());
        return Result.success(body);
    }

    private Map<String, Object> checkDatabase() {
        Map<String, Object> detail = new LinkedHashMap<>();
        try {
            Integer one = jdbcTemplate.queryForObject("SELECT 1", Integer.class);
            boolean ok = one != null && one == 1;
            detail.put("status", ok ? "UP" : "DOWN");
            detail.put("type", "MySQL");
            return detail;
        } catch (Exception e) {
            detail.put("status", "DOWN");
            detail.put("type", "MySQL");
            detail.put("error", rootMessage(e));
            return detail;
        }
    }

    private Map<String, Object> checkRedis() {
        Map<String, Object> detail = new LinkedHashMap<>();
        try {
            String pong = redisTemplate.execute(RedisConnection::ping);
            boolean ok = pong != null && "PONG".equalsIgnoreCase(pong);
            detail.put("status", ok ? "UP" : "DOWN");
            detail.put("type", "Redis");
            return detail;
        } catch (Exception e) {
            detail.put("status", "DOWN");
            detail.put("type", "Redis");
            detail.put("error", rootMessage(e));
            return detail;
        }
    }

    private String rootMessage(Throwable throwable) {
        Throwable current = throwable;
        while (current.getCause() != null && current.getCause() != current) {
            current = current.getCause();
        }
        String message = current.getMessage();
        return message == null ? current.getClass().getSimpleName() : message;
    }
}
