package com.visitor.user;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.visitor.common.enums.RoleEnum;
import com.visitor.domain.entity.User;
import com.visitor.domain.entity.UserRole;
import com.visitor.security.JwtUtil;
import com.visitor.server.mapper.UserMapper;
import com.visitor.server.mapper.UserRoleMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
class UserAdminIntegrationTest {

    @LocalServerPort
    private int port;

    @Autowired
    private TestRestTemplate rest;

    @Autowired
    private UserMapper userMapper;

    @Autowired
    private UserRoleMapper userRoleMapper;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private JwtUtil jwtUtil;

    private String systemToken;
    private String securityToken;

    @BeforeEach
    void setUp() {
        userRoleMapper.delete(null);
        userMapper.delete(null);
        systemToken = token(createUser("13800004001", RoleEnum.SYSTEM_ADMIN));
        securityToken = token(createUser("13800004002", RoleEnum.SECURITY_ADMIN));
    }

    @Test
    void systemAdminCreatesAccountsAndPages() {
        Map<String, Object> created = data(exchange("/web/websuper/users", HttpMethod.POST,
                Map.of("phone", "13900005001", "name", "李老师", "role", 5), systemToken));
        assertThat(created.get("phone")).isEqualTo("13900005001");

        Map<String, Object> page = data(exchange("/web/websuper/users?keyword=李老师",
                HttpMethod.GET, null, systemToken));
        assertThat(((Number) page.get("total")).intValue()).isEqualTo(1);
    }

    @Test
    void duplicatePhoneAndSystemRoleAreRejected() {
        Map<String, Object> dup = exchange("/web/websuper/users", HttpMethod.POST,
                Map.of("phone", "13800004001", "name", "重复", "role", 5), systemToken);
        assertThat(dup.get("code")).isEqualTo(400);

        Map<String, Object> sys = exchange("/web/websuper/users", HttpMethod.POST,
                Map.of("phone", "13900005002", "name", "超管", "role", 1), systemToken);
        assertThat(sys.get("code")).isEqualTo(400);
    }

    @Test
    void securityAdminCannotManageAccounts() {
        Map<String, Object> result = exchange("/web/websuper/users", HttpMethod.GET, null, securityToken);
        assertThat(result.get("code")).isEqualTo(403);
    }

    @Test
    void disableAssignRoleAndResetPassword() {
        Long userId = ((Number) data(exchange("/web/websuper/users", HttpMethod.POST,
                Map.of("phone", "13900005003", "name", "王保安", "role", 3), systemToken)).get("id")).longValue();

        exchange("/web/websuper/users/" + userId + "/status?status=0", HttpMethod.PUT, null, systemToken);
        assertThat(userMapper.selectById(userId).getStatus()).isZero();
        assertThat(userRoleMapper.selectList(new LambdaQueryWrapper<UserRole>()
                .eq(UserRole::getUserId, userId)).get(0).getStatus()).isZero();

        exchange("/web/websuper/users/" + userId + "/role?role=2", HttpMethod.PUT, null, systemToken);
        assertThat(userRoleMapper.selectList(new LambdaQueryWrapper<UserRole>()
                .eq(UserRole::getUserId, userId)).get(0).getRole()).isEqualTo(RoleEnum.SECURITY_ADMIN);

        exchange("/web/websuper/users/" + userId + "/status?status=1", HttpMethod.PUT, null, systemToken);
        exchange("/web/websuper/users/" + userId + "/password?password=newpass123", HttpMethod.PUT, null, systemToken);
        Map<String, Object> login = exchange("/auth/login", HttpMethod.POST,
                Map.of("phone", "13900005003", "password", "newpass123"), null);
        assertThat(login.get("code")).isEqualTo(200);
    }

    private String token(Long userId) {
        return jwtUtil.generate(userId, "user-" + userId, List.of(RoleEnum.SYSTEM_ADMIN),
                RoleEnum.SYSTEM_ADMIN, null);
    }

    private Long createUser(String phone, RoleEnum role) {
        User user = new User();
        user.setPhone(phone);
        user.setName("用户" + phone.substring(phone.length() - 4));
        user.setPassword(passwordEncoder.encode("pass1234"));
        user.setStatus(1);
        user.setCreatedAt(LocalDateTime.now());
        user.setUpdatedAt(LocalDateTime.now());
        userMapper.insert(user);

        UserRole userRole = new UserRole();
        userRole.setUserId(user.getId());
        userRole.setRole(role);
        userRole.setSchoolId(1L);
        userRole.setIsDefault(1);
        userRole.setStatus(1);
        userRole.setCreatedAt(LocalDateTime.now());
        userRole.setUpdatedAt(LocalDateTime.now());
        userRoleMapper.insert(userRole);
        return user.getId();
    }

    private Map<String, Object> exchange(String path, HttpMethod method, Object payload, String token) {
        HttpHeaders headers = new HttpHeaders();
        if (token != null) {
            headers.setBearerAuth(token);
        }
        headers.setContentType(MediaType.APPLICATION_JSON);
        ResponseEntity<Map> response = rest.exchange("http://localhost:" + port + "/api" + path, method,
                new HttpEntity<>(payload, headers), Map.class);
        return response.getBody();
    }

    @SuppressWarnings("unchecked")
    private Map<String, Object> data(Map<String, Object> body) {
        return (Map<String, Object>) body.get("data");
    }
}
