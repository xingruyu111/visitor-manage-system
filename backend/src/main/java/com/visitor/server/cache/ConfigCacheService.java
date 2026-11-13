package com.visitor.server.cache;

import com.fasterxml.jackson.databind.JavaType;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.util.List;

/**
 * 配置类查询缓存。只缓存低频变更数据，Redis 异常时直接回源数据库。
 */
@Slf4j
@Component
public class ConfigCacheService {

    private static final String PREFIX = "vst:cache:";

    private final StringRedisTemplate redisTemplate;
    private final ObjectMapper objectMapper;
    private final boolean enabled;
    private final long ttlSeconds;

    public ConfigCacheService(StringRedisTemplate redisTemplate, ObjectMapper objectMapper,
                              @Value("${app.cache.enabled:true}") boolean enabled,
                              @Value("${app.cache.ttl-seconds:60}") long ttlSeconds) {
        this.redisTemplate = redisTemplate;
        this.objectMapper = objectMapper;
        this.enabled = enabled;
        this.ttlSeconds = ttlSeconds;
    }

    public <T> List<T> getList(String key, Class<T> type) {
        if (!enabled) {
            return null;
        }
        try {
            String json = redisTemplate.opsForValue().get(fullKey(key));
            if (json == null) {
                return null;
            }
            JavaType javaType = objectMapper.getTypeFactory().constructCollectionType(List.class, type);
            return objectMapper.readValue(json, javaType);
        } catch (Exception e) {
            log.warn("读取配置缓存失败，回源数据库: key={}, error={}", key, e.getMessage());
            return null;
        }
    }

    public <T> T getObject(String key, Class<T> type) {
        if (!enabled) {
            return null;
        }
        try {
            String json = redisTemplate.opsForValue().get(fullKey(key));
            return json == null ? null : objectMapper.readValue(json, type);
        } catch (Exception e) {
            log.warn("读取配置缓存失败，回源数据库: key={}, error={}", key, e.getMessage());
            return null;
        }
    }

    public void put(String key, Object value) {
        if (!enabled || value == null) {
            return;
        }
        try {
            redisTemplate.opsForValue().set(fullKey(key), objectMapper.writeValueAsString(value),
                    Duration.ofSeconds(ttlSeconds));
        } catch (Exception e) {
            log.warn("写入配置缓存失败: key={}, error={}", key, e.getMessage());
        }
    }

    public void evict(String... keys) {
        if (!enabled || keys == null) {
            return;
        }
        try {
            String[] fullKeys = new String[keys.length];
            for (int i = 0; i < keys.length; i++) {
                fullKeys[i] = fullKey(keys[i]);
            }
            redisTemplate.delete(List.of(fullKeys));
        } catch (Exception e) {
            log.warn("清除配置缓存失败: error={}", e.getMessage());
        }
    }

    private String fullKey(String key) {
        return PREFIX + key;
    }
}
