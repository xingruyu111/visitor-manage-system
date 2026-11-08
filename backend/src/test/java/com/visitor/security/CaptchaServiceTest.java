package com.visitor.security;

import com.visitor.common.exception.BizException;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.data.redis.RedisConnectionFailureException;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;

import java.time.Duration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class CaptchaServiceTest {

    private final StringRedisTemplate redisTemplate = mock(StringRedisTemplate.class);
    private final ValueOperations<String, String> valueOperations = mock(ValueOperations.class);

    @Test
    void disabledCaptchaSkipsValidation() {
        CaptchaService service = new CaptchaService(redisTemplate, false);

        assertThat(service.isEnabled()).isFalse();
        assertThatCode(() -> service.validate(null, null)).doesNotThrowAnyException();
    }

    @Test
    void generatedCaptchaCanBeValidatedOnce() {
        CaptchaService service = new CaptchaService(redisTemplate, true);
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);

        CaptchaService.Captcha captcha = service.generate();
        assertThat(captcha.captchaId()).isNotBlank();
        assertThat(captcha.image()).startsWith("data:image/png;base64,");

        ArgumentCaptor<String> codeCaptor = ArgumentCaptor.forClass(String.class);
        verify(valueOperations).set(eq("vst:captcha:" + captcha.captchaId()), codeCaptor.capture(),
                eq(Duration.ofMinutes(2)));
        String code = codeCaptor.getValue();

        when(valueOperations.get("vst:captcha:" + captcha.captchaId())).thenReturn(code);
        service.validate(captcha.captchaId(), code.toLowerCase());
        verify(redisTemplate).delete("vst:captcha:" + captcha.captchaId());

        when(valueOperations.get(anyString())).thenReturn(code);
        assertThatThrownBy(() -> service.validate(captcha.captchaId(), "XXXX"))
                .isInstanceOf(BizException.class)
                .hasMessageContaining("验证码错误");
    }

    @Test
    void missingCodeIsRejected() {
        CaptchaService service = new CaptchaService(redisTemplate, true);

        assertThatThrownBy(() -> service.validate("id", " "))
                .isInstanceOf(BizException.class)
                .hasMessageContaining("请输入验证码");
    }

    @Test
    void redisFailureDegradesValidation() {
        CaptchaService service = new CaptchaService(redisTemplate, true);
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        when(valueOperations.get(anyString())).thenThrow(new RedisConnectionFailureException("down"));

        assertThatCode(() -> service.validate("id", "ABCD")).doesNotThrowAnyException();
    }
}
