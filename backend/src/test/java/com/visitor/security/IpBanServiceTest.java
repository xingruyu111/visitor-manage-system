package com.visitor.security;

import com.visitor.common.exception.BizException;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.data.redis.RedisConnectionFailureException;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import java.time.Duration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class IpBanServiceTest {

    private final StringRedisTemplate redisTemplate = mock(StringRedisTemplate.class);
    private final ValueOperations<String, String> valueOperations = mock(ValueOperations.class);
    private final IpBanService service = new IpBanService(redisTemplate, 2, 15, 10);

    @BeforeEach
    void setUp() {
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        setRequest("1.1.1.1", null);
    }

    @AfterEach
    void tearDown() {
        RequestContextHolder.resetRequestAttributes();
    }

    @Test
    void allowsWhenNotBanned() {
        when(redisTemplate.hasKey("vst:ip:ban:1.1.1.1")).thenReturn(false);

        assertThatCode(service::assertAllowed).doesNotThrowAnyException();
    }

    @Test
    void blocksWhenBanned() {
        when(redisTemplate.hasKey("vst:ip:ban:1.1.1.1")).thenReturn(true);

        assertThatThrownBy(service::assertAllowed)
                .isInstanceOf(BizException.class)
                .hasMessageContaining("临时封禁");
    }

    @Test
    void degradesWhenRedisFails() {
        when(redisTemplate.hasKey(anyString())).thenThrow(new RedisConnectionFailureException("down"));

        assertThatCode(service::assertAllowed).doesNotThrowAnyException();
        assertThatCode(service::recordFailure).doesNotThrowAnyException();
        assertThatCode(service::clear).doesNotThrowAnyException();
    }

    @Test
    void failureAtThresholdBansIp() {
        when(valueOperations.increment("vst:ip:fail:1.1.1.1")).thenReturn(1L, 2L);

        service.recordFailure();
        verify(redisTemplate).expire("vst:ip:fail:1.1.1.1", Duration.ofMinutes(10));
        service.recordFailure();

        verify(valueOperations).set("vst:ip:ban:1.1.1.1", "1", Duration.ofMinutes(15));
    }

    @Test
    void clearDeletesFailureCounter() {
        service.clear();

        verify(redisTemplate).delete("vst:ip:fail:1.1.1.1");
    }

    @Test
    void resolvesForwardedAndDirectIp() {
        setRequest("10.0.0.1", "2.2.2.2, 3.3.3.3");
        assertThat(service.currentIp()).isEqualTo("2.2.2.2");

        setRequest("10.0.0.1", null);
        assertThat(service.currentIp()).isEqualTo("10.0.0.1");

        RequestContextHolder.resetRequestAttributes();
        assertThat(service.currentIp()).isNull();
        assertThatCode(service::assertAllowed).doesNotThrowAnyException();
    }

    private void setRequest(String remoteAddr, String forwarded) {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setRemoteAddr(remoteAddr);
        if (forwarded != null) {
            request.addHeader("X-Forwarded-For", forwarded);
        }
        RequestContextHolder.setRequestAttributes(new ServletRequestAttributes(request));
    }
}
