package com.visitor.security;

import com.visitor.common.exception.BizException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

import javax.imageio.ImageIO;
import java.awt.Color;
import java.awt.Font;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.time.Duration;
import java.util.Base64;
import java.util.Random;
import java.util.UUID;

/**
 * 图形验证码：登录可选开启（security.captcha.enabled=true）。
 * 答案存 Redis 2 分钟，校验后立即失效。
 */
@Slf4j
@Component
public class CaptchaService {

    private static final String KEY_PREFIX = "vst:captcha:";
    private static final String CHARS = "ABCDEFGHJKLMNPQRSTUVWXYZ23456789";

    private final StringRedisTemplate redisTemplate;
    private final boolean enabled;

    public CaptchaService(StringRedisTemplate redisTemplate,
                          @Value("${security.captcha.enabled:false}") boolean enabled) {
        this.redisTemplate = redisTemplate;
        this.enabled = enabled;
    }

    public boolean isEnabled() {
        return enabled;
    }

    public Captcha generate() {
        String code = randomCode(4);
        String id = UUID.randomUUID().toString().replace("-", "");
        try {
            redisTemplate.opsForValue().set(KEY_PREFIX + id, code, Duration.ofMinutes(2));
        } catch (Exception e) {
            log.warn("验证码写入 Redis 失败: {}", e.getMessage());
            throw new BizException(500, "验证码服务暂不可用");
        }
        return new Captcha(id, "data:image/png;base64," + render(code));
    }

    /** 校验并一次性失效。 */
    public void validate(String captchaId, String captchaCode) {
        if (!enabled) {
            return;
        }
        if (captchaId == null || captchaCode == null || captchaCode.isBlank()) {
            throw new BizException(400, "请输入验证码");
        }
        try {
            String expected = redisTemplate.opsForValue().get(KEY_PREFIX + captchaId);
            redisTemplate.delete(KEY_PREFIX + captchaId);
            if (expected == null || !expected.equalsIgnoreCase(captchaCode.trim())) {
                throw new BizException(400, "验证码错误或已过期");
            }
        } catch (BizException e) {
            throw e;
        } catch (Exception e) {
            log.warn("验证码校验失败（降级放行）: {}", e.getMessage());
        }
    }

    private String randomCode(int length) {
        Random random = new Random();
        StringBuilder builder = new StringBuilder();
        for (int i = 0; i < length; i++) {
            builder.append(CHARS.charAt(random.nextInt(CHARS.length())));
        }
        return builder.toString();
    }

    private String render(String code) {
        int width = 120;
        int height = 40;
        BufferedImage image = new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB);
        Graphics2D graphics = image.createGraphics();
        graphics.setColor(new Color(245, 247, 250));
        graphics.fillRect(0, 0, width, height);
        graphics.setFont(new Font("Arial", Font.BOLD, 24));
        Random random = new Random();
        for (int i = 0; i < code.length(); i++) {
            graphics.setColor(new Color(random.nextInt(120), random.nextInt(120), random.nextInt(120)));
            graphics.drawString(String.valueOf(code.charAt(i)), 15 + i * 25, 28);
        }
        for (int i = 0; i < 6; i++) {
            graphics.setColor(new Color(random.nextInt(200), random.nextInt(200), random.nextInt(200)));
            graphics.drawLine(random.nextInt(width), random.nextInt(height), random.nextInt(width), random.nextInt(height));
        }
        graphics.dispose();
        try (ByteArrayOutputStream output = new ByteArrayOutputStream()) {
            ImageIO.write(image, "png", output);
            return Base64.getEncoder().encodeToString(output.toByteArray());
        } catch (Exception e) {
            throw new BizException(500, "验证码生成失败");
        }
    }

    public record Captcha(String captchaId, String image) {
    }
}
