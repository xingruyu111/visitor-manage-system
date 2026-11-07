package com.visitor.security;

import com.visitor.common.exception.BizException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

import java.time.Duration;

/**
 * 登录失败次数限制：同一手机号连续失败达到阈值后锁定一段时间。
 *
 * <p>计数存 Redis（vst:login:fail:&lt;phone&gt;），成功登录后清零；Redis 故障时降级放行。</p>
 */
@Component
public class LoginAttemptService {

    private static final Logger log = LoggerFactory.getLogger(LoginAttemptService.class);
    private static final String KEY_PREFIX = "vst:login:fail:";

    private final StringRedisTemplate redisTemplate;
    private final int maxAttempts;
    private final int lockMinutes;

    public LoginAttemptService(StringRedisTemplate redisTemplate,
                               @Value("${security.login.max-attempts:5}") int maxAttempts,
                               @Value("${security.login.lock-minutes:10}") int lockMinutes) {
        this.redisTemplate = redisTemplate;
        this.maxAttempts = maxAttempts;
        this.lockMinutes = lockMinutes;
    }

    /** 登录前调用：已锁定则直接抛 429。 */
    public void assertAllowed(String phone) {
        try {
            String value = redisTemplate.opsForValue().get(KEY_PREFIX + phone);
            if (value != null && Integer.parseInt(value) >= maxAttempts) {
                throw new BizException(429, "尝试过于频繁，请 " + lockMinutes + " 分钟后再试");
            }
        } catch (BizException e) {
            throw e;
        } catch (Exception e) {
            log.warn("登录限流查询失败（降级放行）: {}", e.getMessage());
        }
    }

    /** 密码错误时调用。 */
    public void recordFailure(String phone) {
        try {
            Long count = redisTemplate.opsForValue().increment(KEY_PREFIX + phone);
            if (count != null && count == 1L) {
                redisTemplate.expire(KEY_PREFIX + phone, Duration.ofMinutes(lockMinutes));
            }
        } catch (Exception e) {
            log.warn("登录失败计数写入失败（降级）: {}", e.getMessage());
        }
    }

    /** 登录成功后清零。 */
    public void clear(String phone) {
        try {
            redisTemplate.delete(KEY_PREFIX + phone);
        } catch (Exception e) {
            log.warn("登录失败计数清理失败（降级）: {}", e.getMessage());
        }
    }

    public int getMaxAttempts() {
        return maxAttempts;
    }
}
