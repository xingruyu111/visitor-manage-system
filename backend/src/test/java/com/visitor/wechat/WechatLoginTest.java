package com.visitor.wechat;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.visitor.common.enums.RoleEnum;
import com.visitor.common.exception.BizException;
import com.visitor.domain.dto.WechatLoginDTO;
import com.visitor.domain.entity.MpConfig;
import com.visitor.domain.entity.User;
import com.visitor.domain.entity.UserRole;
import com.visitor.server.mapper.MpConfigMapper;
import com.visitor.server.mapper.UserMapper;
import com.visitor.server.mapper.UserRoleMapper;
import com.visitor.server.service.WechatLoginService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import java.time.LocalDateTime;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@SpringBootTest
@ActiveProfiles("test")
class WechatLoginTest {

    @Autowired
    private WechatLoginService service;

    @Autowired
    private MpConfigMapper mpConfigMapper;

    @Autowired
    private UserMapper userMapper;

    @Autowired
    private UserRoleMapper userRoleMapper;

    @BeforeEach
    void setUp() {
        mpConfigMapper.delete(null);
        userRoleMapper.delete(null);
        userMapper.delete(null);
    }

    @Test
    void loginRequiresConfiguration() {
        assertThatThrownBy(() -> service.login(dto("code", "13900001234")))
                .isInstanceOf(BizException.class)
                .hasMessageContaining("未配置");
    }

    @Test
    void mockLoginCreatesVisitorAndReusesUser() {
        insertConfig("MOCK");

        Map<String, Object> first = service.login(dto("abc", "13900001234"));
        Long userId = ((Number) first.get("userId")).longValue();
        assertThat(first.get("token").toString()).isNotBlank();
        assertThat(first.get("currentRole")).isEqualTo(RoleEnum.VISITOR.getCode());

        User user = userMapper.selectById(userId);
        assertThat(user.getMpOpenid()).isEqualTo("mock-abc");
        assertThat(userRoleMapper.selectList(new LambdaQueryWrapper<UserRole>()
                .eq(UserRole::getUserId, userId))).hasSize(1);

        Map<String, Object> second = service.login(dto("abc", "13900001234"));
        assertThat(((Number) second.get("userId")).longValue()).isEqualTo(userId);
        assertThat(userMapper.selectCount(null)).isEqualTo(1L);
    }

    @Test
    void existingPhoneIsBoundWithoutCreatingUser() {
        insertConfig("MOCK");
        User teacher = new User();
        teacher.setPhone("13900005678");
        teacher.setName("老师");
        teacher.setPassword("x");
        teacher.setStatus(1);
        teacher.setCreatedAt(LocalDateTime.now());
        teacher.setUpdatedAt(LocalDateTime.now());
        userMapper.insert(teacher);
        UserRole role = new UserRole();
        role.setUserId(teacher.getId());
        role.setRole(RoleEnum.TEACHER);
        role.setSchoolId(1L);
        role.setIsDefault(1);
        role.setStatus(1);
        role.setCreatedAt(LocalDateTime.now());
        role.setUpdatedAt(LocalDateTime.now());
        userRoleMapper.insert(role);

        Map<String, Object> result = service.login(dto("xyz", "13900005678"));

        assertThat(((Number) result.get("userId")).longValue()).isEqualTo(teacher.getId());
        assertThat(result.get("currentRole")).isEqualTo(RoleEnum.TEACHER.getCode());
        assertThat(userMapper.selectById(teacher.getId()).getMpOpenid()).isEqualTo("mock-xyz");
    }

    private WechatLoginDTO dto(String code, String phone) {
        WechatLoginDTO dto = new WechatLoginDTO();
        dto.setCode(code);
        dto.setPhone(phone);
        return dto;
    }

    private void insertConfig(String mode) {
        MpConfig config = new MpConfig();
        config.setAppId("wx-test");
        config.setSecret("secret");
        config.setAuthMode(mode);
        config.setCreatedAt(LocalDateTime.now());
        config.setUpdatedAt(LocalDateTime.now());
        mpConfigMapper.insert(config);
    }
}
