package com.visitor.security;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.data.redis.RedisConnectionFailureException;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;

import java.time.Duration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class TokenBlacklistTest {

    private final StringRedisTemplate redisTemplate = mock(StringRedisTemplate.class);
    private final ValueOperations<String, String> valueOperations = mock(ValueOperations.class);
    private final TokenBlacklist blacklist = new TokenBlacklist(redisTemplate);

    @BeforeEach
    void setUp() {
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
    }

    @Test
    void addStoresKeyWithTtl() {
        blacklist.add("jti-1", 60);

        verify(valueOperations).set(eq("vst:token:blacklist:jti-1"), eq("1"), eq(Duration.ofSeconds(60)));
    }

    @Test
    void containsReturnsTrueForBlacklistedToken() {
        when(redisTemplate.hasKey("vst:token:blacklist:jti-1")).thenReturn(true);

        assertThat(blacklist.contains("jti-1")).isTrue();
    }

    @Test
    void containsReturnsFalseWhenKeyMissing() {
        when(redisTemplate.hasKey("vst:token:blacklist:jti-1")).thenReturn(false);

        assertThat(blacklist.contains("jti-1")).isFalse();
    }

    @Test
    void redisFailureFallsBackToAllow() {
        when(redisTemplate.hasKey(anyString())).thenThrow(new RedisConnectionFailureException("down"));

        assertThat(blacklist.contains("jti-1")).isFalse();
    }

    @Test
    void addSwallowsRedisFailure() {
        when(redisTemplate.opsForValue()).thenThrow(new RedisConnectionFailureException("down"));

        assertThatCode(() -> blacklist.add("jti-1", 60)).doesNotThrowAnyException();
    }

    @Test
    void nullJtiIsIgnored() {
        blacklist.add(null, 60);

        assertThat(blacklist.contains(null)).isFalse();
        verify(valueOperations, never()).set(anyString(), anyString(), any(Duration.class));
    }

    @Test
    void nonPositiveTtlIsIgnored() {
        blacklist.add("jti-1", 0);

        verify(valueOperations, never()).set(anyString(), anyString(), any(Duration.class));
    }
}
