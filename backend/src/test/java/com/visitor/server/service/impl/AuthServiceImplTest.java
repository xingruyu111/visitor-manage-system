package com.visitor.server.service.impl;

import com.visitor.common.enums.RoleEnum;
import com.visitor.common.exception.BizException;
import com.visitor.domain.entity.User;
import com.visitor.domain.entity.UserRole;
import com.visitor.domain.vo.LoginVO;
import com.visitor.integration.IntegrationFactory;
import com.visitor.integration.sso.IdentityProvider;
import com.visitor.security.JwtUtil;
import com.visitor.security.TokenBlacklist;
import com.visitor.server.mapper.UserMapper;
import com.visitor.server.mapper.UserRoleMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class AuthServiceImplTest {

    private final UserMapper userMapper = mock(UserMapper.class);
    private final UserRoleMapper userRoleMapper = mock(UserRoleMapper.class);
    private final PasswordEncoder passwordEncoder = mock(PasswordEncoder.class);
    private final JwtUtil jwtUtil = mock(JwtUtil.class);
    private final TokenBlacklist tokenBlacklist = mock(TokenBlacklist.class);
    private final IntegrationFactory integrationFactory = mock(IntegrationFactory.class);

    private final AuthServiceImpl service = new AuthServiceImpl(userMapper, userRoleMapper,
            passwordEncoder, jwtUtil, tokenBlacklist, integrationFactory);

    @BeforeEach
    void setUp() {
        when(jwtUtil.generate(anyLong(), anyString(), anyList(), any(), any())).thenReturn("token");
        when(jwtUtil.getExpirationMs()).thenReturn(60000L);
    }

    @Test
    void meThrowsWhenUserMissing() {
        when(userMapper.selectById(9L)).thenReturn(null);

        assertThatThrownBy(() -> service.me(9L))
                .isInstanceOf(BizException.class)
                .hasMessage("用户不存在");
    }

    @Test
    void logoutWithInvalidTokenDoesNotThrow() {
        when(jwtUtil.parse("bad")).thenThrow(new RuntimeException("bad token"));

        assertThatCode(() -> service.logout("bad")).doesNotThrowAnyException();
    }

    @Test
    void loginPicksFirstRoleWhenNoDefaultFlag() {
        User user = user(1L, 1);
        when(userMapper.selectOne(any())).thenReturn(user);
        when(passwordEncoder.matches("pass", "hash")).thenReturn(true);
        when(user.getPassword()).thenReturn("hash");
        when(userRoleMapper.selectList(any())).thenReturn(List.of(role(1L, RoleEnum.VISITOR, 0)));

        LoginVO vo = service.login("13700000010", "pass", "127.0.0.1");

        assertThat(vo.getCurrentRole()).isEqualTo(RoleEnum.VISITOR.getCode());
        assertThat(vo.getToken()).isEqualTo("token");
    }

    @Test
    void ssoLoginRejectsDisabledUser() {
        IdentityProvider provider = mock(IdentityProvider.class);
        when(integrationFactory.identity()).thenReturn(provider);
        when(provider.isConfigured()).thenReturn(true);
        when(provider.exchange("code", "redirect")).thenReturn(new IdentityProvider.SsoUser(
                "ext-1", "ext", "外部用户", "13700000099", null, null, "TEACHER", Map.of()));
        User disabled = user(5L, 0);
        when(userMapper.selectOne(any())).thenReturn(disabled);

        assertThatThrownBy(() -> service.ssoLogin("code", "redirect"))
                .isInstanceOf(BizException.class)
                .hasMessage("账号已停用");
    }

    private User user(Long id, int status) {
        User user = mock(User.class);
        when(user.getId()).thenReturn(id);
        when(user.getName()).thenReturn("用户" + id);
        when(user.getPhone()).thenReturn("13700000010");
        when(user.getStatus()).thenReturn(status);
        return user;
    }

    private UserRole role(Long userId, RoleEnum role, int isDefault) {
        UserRole userRole = new UserRole();
        userRole.setUserId(userId);
        userRole.setRole(role);
        userRole.setIsDefault(isDefault);
        userRole.setStatus(1);
        return userRole;
    }
}
