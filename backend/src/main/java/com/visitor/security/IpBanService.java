package com.visitor.security;

import com.visitor.common.exception.BizException;
import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import java.time.Duration;

/**
 * IP 临时封禁：同一 IP 连续登录失败达到阈值后封禁一段时间。
 * Redis 故障时降级放行。
 */
@Slf4j
@Component
public class IpBanService {

    private static final String FAIL_PREFIX = "vst:ip:fail:";
    private static final String BAN_PREFIX = "vst:ip:ban:";

    private final StringRedisTemplate redisTemplate;
    private final int maxFailures;
    private final int banMinutes;
    private final int windowMinutes;

    public IpBanService(StringRedisTemplate redisTemplate,
                        @Value("${security.ip-ban.max-failures:20}") int maxFailures,
                        @Value("${security.ip-ban.ban-minutes:15}") int banMinutes,
                        @Value("${security.ip-ban.window-minutes:10}") int windowMinutes) {
        this.redisTemplate = redisTemplate;
        this.maxFailures = maxFailures;
        this.banMinutes = banMinutes;
        this.windowMinutes = windowMinutes;
    }

    public void assertAllowed() {
        String ip = currentIp();
        if (ip == null) {
            return;
        }
        try {
            if (Boolean.TRUE.equals(redisTemplate.hasKey(BAN_PREFIX + ip))) {
                throw new BizException(429, "访问过于频繁，IP 已被临时封禁");
            }
        } catch (BizException e) {
            throw e;
        } catch (Exception e) {
            log.warn("IP 封禁查询失败（降级放行）: {}", e.getMessage());
        }
    }

    public void recordFailure() {
        String ip = currentIp();
        if (ip == null) {
            return;
        }
        try {
            Long count = redisTemplate.opsForValue().increment(FAIL_PREFIX + ip);
            if (count != null && count == 1L) {
                redisTemplate.expire(FAIL_PREFIX + ip, Duration.ofMinutes(windowMinutes));
            }
            if (count != null && count >= maxFailures) {
                redisTemplate.opsForValue().set(BAN_PREFIX + ip, "1", Duration.ofMinutes(banMinutes));
                log.warn("IP 临时封禁: ip={}", ip);
            }
        } catch (Exception e) {
            log.warn("IP 失败计数写入失败（降级）: {}", e.getMessage());
        }
    }

    public void clear() {
        String ip = currentIp();
        if (ip == null) {
            return;
        }
        try {
            redisTemplate.delete(FAIL_PREFIX + ip);
        } catch (Exception e) {
            log.warn("IP 失败计数清理失败（降级）: {}", e.getMessage());
        }
    }

    public String currentIp() {
        try {
            ServletRequestAttributes attributes =
                    (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
            if (attributes == null) {
                return null;
            }
            HttpServletRequest request = attributes.getRequest();
            String forwarded = request.getHeader("X-Forwarded-For");
            if (forwarded != null && !forwarded.isBlank()) {
                return forwarded.split(",")[0].trim();
            }
            return request.getRemoteAddr();
        } catch (Exception e) {
            return null;
        }
    }

    public int getMaxFailures() {
        return maxFailures;
    }
}
