package com.visitor.security;

import com.visitor.common.enums.RoleEnum;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.JwtException;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class JwtUtilTest {

    private static final String SECRET = "unit-test-secret-key-1234567890-abcdef";

    private final JwtUtil jwtUtil = new JwtUtil(SECRET, 60000);

    @Test
    void generateAndParseCarriesClaims() {
        String token = jwtUtil.generate(7L, "张三", List.of(RoleEnum.SYSTEM_ADMIN, RoleEnum.VISITOR),
                RoleEnum.SYSTEM_ADMIN, 9L);

        Claims claims = jwtUtil.parse(token);

        assertThat(claims.getSubject()).isEqualTo("7");
        assertThat(claims.get("name")).isEqualTo("张三");
        assertThat(claims.get("role", Integer.class)).isEqualTo(RoleEnum.SYSTEM_ADMIN.getCode());
        assertThat(claims.get("roles")).isEqualTo("1,4");
        assertThat(((Number) claims.get("schoolId")).longValue()).isEqualTo(9L);
        assertThat(claims.getId()).isNotBlank();
        assertThat(claims.getExpiration()).isAfter(claims.getIssuedAt());
    }

    @Test
    void expiredTokenIsRejected() {
        JwtUtil expired = new JwtUtil(SECRET, -1000);
        String token = expired.generate(1L, "过期", List.of(RoleEnum.VISITOR), RoleEnum.VISITOR, null);

        assertThatThrownBy(() -> expired.parse(token)).isInstanceOf(ExpiredJwtException.class);
    }

    @Test
    void tamperedTokenIsRejected() {
        String token = jwtUtil.generate(1L, "篡改", List.of(RoleEnum.VISITOR), RoleEnum.VISITOR, null);

        assertThatThrownBy(() -> jwtUtil.parse(token + "x")).isInstanceOf(JwtException.class);
    }

    @Test
    void tokenSignedByAnotherSecretIsRejected() {
        JwtUtil other = new JwtUtil("another-secret-key-0987654321-zyxwvu", 60000);
        String token = other.generate(1L, "别人", List.of(RoleEnum.VISITOR), RoleEnum.VISITOR, null);

        assertThatThrownBy(() -> jwtUtil.parse(token)).isInstanceOf(JwtException.class);
    }
}
