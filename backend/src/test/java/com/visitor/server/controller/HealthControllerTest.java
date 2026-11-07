package com.visitor.server.controller;

import com.visitor.common.core.Result;
import org.junit.jupiter.api.Test;
import org.springframework.data.redis.RedisConnectionFailureException;
import org.springframework.data.redis.core.RedisCallback;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.jdbc.CannotGetJdbcConnectionException;
import org.springframework.jdbc.core.JdbcTemplate;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class HealthControllerTest {

    private final JdbcTemplate jdbcTemplate = mock(JdbcTemplate.class);
    private final StringRedisTemplate redisTemplate = mock(StringRedisTemplate.class);
    private final HealthController controller = new HealthController(jdbcTemplate, redisTemplate);

    @Test
    void reportsUpWhenBothDependenciesHealthy() {
        when(jdbcTemplate.queryForObject("SELECT 1", Integer.class)).thenReturn(1);
        when(redisTemplate.execute(any(RedisCallback.class))).thenReturn("PONG");

        Result<Map<String, Object>> result = controller.health();

        assertThat(result.data().get("status")).isEqualTo("UP");
        assertThat(statusOf(result, "database")).isEqualTo("UP");
        assertThat(statusOf(result, "redis")).isEqualTo("UP");
    }

    @Test
    void reportsDegradedWhenDatabaseDown() {
        when(jdbcTemplate.queryForObject(eq("SELECT 1"), eq(Integer.class)))
                .thenThrow(new CannotGetJdbcConnectionException("db down"));
        when(redisTemplate.execute(any(RedisCallback.class))).thenReturn("PONG");

        Result<Map<String, Object>> result = controller.health();

        assertThat(result.data().get("status")).isEqualTo("DEGRADED");
        assertThat(statusOf(result, "database")).isEqualTo("DOWN");
        assertThat(statusOf(result, "redis")).isEqualTo("UP");
    }

    @Test
    void reportsDegradedWhenRedisDown() {
        when(jdbcTemplate.queryForObject("SELECT 1", Integer.class)).thenReturn(1);
        when(redisTemplate.execute(any(RedisCallback.class)))
                .thenThrow(new RedisConnectionFailureException("redis down"));

        Result<Map<String, Object>> result = controller.health();

        assertThat(result.data().get("status")).isEqualTo("DEGRADED");
        assertThat(statusOf(result, "database")).isEqualTo("UP");
        assertThat(statusOf(result, "redis")).isEqualTo("DOWN");
    }

    @SuppressWarnings("unchecked")
    private String statusOf(Result<Map<String, Object>> result, String key) {
        return (String) ((Map<String, Object>) result.data().get(key)).get("status");
    }
}
