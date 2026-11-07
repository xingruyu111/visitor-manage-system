package com.visitor.security;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

import java.time.Duration;

/**
 * 令牌黑名单：登出后按 jti 记录到 Redis，过期时间与令牌剩余有效期一致。
 *
 * <p>Redis 不可用时降级为“不拦截”，只记录告警，避免缓存故障导致全站登录失败。</p>
 */
@Component
public class TokenBlacklist {

    private static final Logger log = LoggerFactory.getLogger(TokenBlacklist.class);
    private static final String KEY_PREFIX = "vst:token:blacklist:";

    private final StringRedisTemplate redisTemplate;

    public TokenBlacklist(StringRedisTemplate redisTemplate) {
        this.redisTemplate = redisTemplate;
    }

    public void add(String jti, long ttlSeconds) {
        if (jti == null || ttlSeconds <= 0) {
            return;
        }
        try {
            redisTemplate.opsForValue().set(KEY_PREFIX + jti, "1", Duration.ofSeconds(ttlSeconds));
        } catch (Exception e) {
            log.warn("写入令牌黑名单失败（降级放行）: {}", e.getMessage());
        }
    }

    public boolean contains(String jti) {
        if (jti == null) {
            return false;
        }
        try {
            return Boolean.TRUE.equals(redisTemplate.hasKey(KEY_PREFIX + jti));
        } catch (Exception e) {
            log.warn("查询令牌黑名单失败（降级放行）: {}", e.getMessage());
            return false;
        }
    }
}
