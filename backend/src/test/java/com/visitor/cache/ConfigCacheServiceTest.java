package com.visitor.cache;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.visitor.server.cache.ConfigCacheService;
import org.junit.jupiter.api.Test;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;

import java.time.Duration;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class ConfigCacheServiceTest {

    @SuppressWarnings("unchecked")
    private final StringRedisTemplate redis = mock(StringRedisTemplate.class);
    @SuppressWarnings("unchecked")
    private final ValueOperations<String, String> ops = mock(ValueOperations.class);
    private final ObjectMapper objectMapper = new ObjectMapper();

    @Test
    void readsAndWritesJsonWithTtl() {
        when(redis.opsForValue()).thenReturn(ops);
        when(ops.get("vst:cache:demo")).thenReturn("[\"A\",\"B\"]");
        ConfigCacheService cache = new ConfigCacheService(redis, objectMapper, true, 30);

        assertThat(cache.getList("demo", String.class)).containsExactly("A", "B");

        cache.put("demo", List.of("C"));
        verify(ops).set(eq("vst:cache:demo"), eq("[\"C\"]"), eq(Duration.ofSeconds(30)));

        cache.evict("demo", "other");
        verify(redis).delete(anyList());
    }

    @Test
    void redisFailureFallsBackToNull() {
        when(redis.opsForValue()).thenReturn(ops);
        when(ops.get(any())).thenThrow(new RuntimeException("redis down"));
        ConfigCacheService cache = new ConfigCacheService(redis, objectMapper, true, 30);

        assertThat(cache.getList("demo", String.class)).isNull();
        assertThat(cache.getObject("demo", String.class)).isNull();
    }

    @Test
    void disabledCacheDoesNotTouchRedis() {
        ConfigCacheService cache = new ConfigCacheService(redis, objectMapper, false, 30);

        assertThat(cache.getList("demo", String.class)).isNull();
        cache.put("demo", List.of("A"));
        cache.evict("demo");
    }
}
