package com.visitor.security;

import com.visitor.common.enums.RoleEnum;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Date;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * JWT 签发与解析。密钥来自 JWT_SECRET（至少 32 字符）。
 */
@Component
public class JwtUtil {

    private static final Logger log = LoggerFactory.getLogger(JwtUtil.class);
    private static final String DEV_SECRET = "dev-only-jwt-secret-please-change-in-production";

    private final SecretKey key;
    private final long expirationMs;

    public JwtUtil(@Value("${jwt.secret:}") String secret,
                   @Value("${jwt.expiration:86400000}") long expirationMs) {
        String source = secret;
        if (source == null || source.length() < 32) {
            log.warn("[安全] 未配置 JWT_SECRET 或长度不足 32，使用开发默认密钥；生产环境必须配置");
            source = DEV_SECRET;
        }
        this.key = Keys.hmacShaKeyFor(source.getBytes(StandardCharsets.UTF_8));
        this.expirationMs = expirationMs;
    }

    public String generate(Long userId, String name, List<RoleEnum> roles,
                           RoleEnum currentRole, Long schoolId) {
        Instant now = Instant.now();
        var builder = Jwts.builder()
                .subject(String.valueOf(userId))
                .id(UUID.randomUUID().toString())
                .claim("name", name)
                .claim("roles", roles.stream().map(RoleEnum::getCode).map(String::valueOf)
                        .collect(Collectors.joining(",")))
                .claim("role", currentRole.getCode())
                .issuedAt(Date.from(now))
                .expiration(Date.from(now.plusMillis(expirationMs)))
                .signWith(key);
        if (schoolId != null) {
            builder.claim("schoolId", schoolId);
        }
        return builder.compact();
    }

    public Claims parse(String token) {
        return Jwts.parser().verifyWith(key).build().parseSignedClaims(token).getPayload();
    }

    public long getExpirationMs() {
        return expirationMs;
    }
}
