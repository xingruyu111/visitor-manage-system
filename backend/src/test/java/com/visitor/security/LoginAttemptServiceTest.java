package com.visitor.security;

import com.visitor.common.exception.BizException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.data.redis.RedisConnectionFailureException;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;

import java.time.Duration;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class LoginAttemptServiceTest {

    private final StringRedisTemplate redisTemplate = mock(StringRedisTemplate.class);
    private final ValueOperations<String, String> valueOperations = mock(ValueOperations.class);
    private final LoginAttemptService service = new LoginAttemptService(redisTemplate, 5, 10);

    @BeforeEach
    void setUp() {
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
    }

    @Test
    void allowsLoginWhenNoFailuresRecorded() {
        when(valueOperations.get("vst:login:fail:13700000010")).thenReturn(null);

        assertThatCode(() -> service.assertAllowed("13700000010")).doesNotThrowAnyException();
    }

    @Test
    void allowsLoginBelowThreshold() {
        when(valueOperations.get("vst:login:fail:13700000010")).thenReturn("4");

        assertThatCode(() -> service.assertAllowed("13700000010")).doesNotThrowAnyException();
    }

    @Test
    void blocksLoginAtThreshold() {
        when(valueOperations.get("vst:login:fail:13700000010")).thenReturn("5");

        assertThatThrownBy(() -> service.assertAllowed("13700000010"))
                .isInstanceOf(BizException.class)
                .hasMessageContaining("尝试过于频繁");
    }

    @Test
    void firstFailureSetsExpiry() {
        when(valueOperations.increment("vst:login:fail:13700000010")).thenReturn(1L);

        service.recordFailure("13700000010");

        verify(redisTemplate).expire(eq("vst:login:fail:13700000010"), eq(Duration.ofMinutes(10)));
    }

    @Test
    void subsequentFailureKeepsExistingTtl() {
        when(valueOperations.increment("vst:login:fail:13700000010")).thenReturn(2L);

        service.recordFailure("13700000010");

        verify(redisTemplate, never()).expire(anyString(), eq(Duration.ofMinutes(10)));
    }

    @Test
    void clearDeletesCounter() {
        service.clear("13700000010");

        verify(redisTemplate).delete("vst:login:fail:13700000010");
    }

    @Test
    void redisFailureFailsOpen() {
        when(valueOperations.get(anyString())).thenThrow(new RedisConnectionFailureException("down"));
        when(valueOperations.increment(anyString())).thenThrow(new RedisConnectionFailureException("down"));
        when(redisTemplate.delete(anyString())).thenThrow(new RedisConnectionFailureException("down"));

        assertThatCode(() -> service.assertAllowed("13700000010")).doesNotThrowAnyException();
        assertThatCode(() -> service.recordFailure("13700000010")).doesNotThrowAnyException();
        assertThatCode(() -> service.clear("13700000010")).doesNotThrowAnyException();
    }
}
