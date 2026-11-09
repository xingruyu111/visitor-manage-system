package com.visitor.user;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.visitor.common.enums.RoleEnum;
import com.visitor.domain.entity.ApprovalTask;
import com.visitor.domain.entity.User;
import com.visitor.domain.entity.UserRole;
import com.visitor.security.JwtUtil;
import com.visitor.server.mapper.ApprovalTaskMapper;
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
    private ApprovalTaskMapper taskMapper;

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
    void addRemoveRoleDisableAndResetPassword() {
        Long userId = ((Number) data(exchange("/web/websuper/users", HttpMethod.POST,
                Map.of("phone", "13900005003", "name", "王保安", "role", 3), systemToken)).get("id")).longValue();

        exchange("/web/websuper/users/" + userId + "/roles?role=2", HttpMethod.POST, null, systemToken);
        assertThat(userRoleMapper.selectList(new LambdaQueryWrapper<UserRole>()
                .eq(UserRole::getUserId, userId))).hasSize(2);

        exchange("/web/websuper/users/" + userId + "/roles/3", HttpMethod.DELETE, null, systemToken);
        assertThat(userRoleMapper.selectList(new LambdaQueryWrapper<UserRole>()
                .eq(UserRole::getUserId, userId))).hasSize(1);

        exchange("/web/websuper/users/" + userId + "/status?status=0", HttpMethod.PUT, null, systemToken);
        assertThat(userMapper.selectById(userId).getStatus()).isZero();
        assertThat(userRoleMapper.selectList(new LambdaQueryWrapper<UserRole>()
                .eq(UserRole::getUserId, userId)).get(0).getStatus()).isEqualTo(1);

        exchange("/web/websuper/users/" + userId + "/status?status=1", HttpMethod.PUT, null, systemToken);
        exchange("/web/websuper/users/" + userId + "/password?password=newpass123", HttpMethod.PUT, null, systemToken);
        Map<String, Object> login = exchange("/auth/login", HttpMethod.POST,
                Map.of("phone", "13900005003", "password", "newpass123"), null);
        assertThat(login.get("code")).isEqualTo(200);
    }

    @Test
    void disableWithPendingTaskIsRejected() {
        Long userId = ((Number) data(exchange("/web/websuper/users", HttpMethod.POST,
                Map.of("phone", "13900005004", "name", "李审批", "role", 5), systemToken)).get("id")).longValue();

        ApprovalTask task = new ApprovalTask();
        task.setInstanceId(1L);
        task.setSeq(1);
        task.setNodeCode("N1");
        task.setAssigneeType("TARGET");
        task.setAssigneeId(userId);
        task.setStatus("PENDING");
        task.setCreatedAt(LocalDateTime.now());
        taskMapper.insert(task);

        Map<String, Object> blocked = exchange("/web/websuper/users/" + userId + "/status?status=0",
                HttpMethod.PUT, null, systemToken);
        assertThat(blocked.get("code")).isEqualTo(400);

        taskMapper.deleteById(task.getId());
        exchange("/web/websuper/users/" + userId + "/status?status=0", HttpMethod.PUT, null, systemToken);
        assertThat(userMapper.selectById(userId).getStatus()).isZero();
    }

    @Test
    void accountPhoneCanBeChangedIndependently() {
        Long userId = ((Number) data(exchange("/web/websuper/users", HttpMethod.POST,
                Map.of("phone", "13900005005", "name", "赵老师", "role", 5), systemToken)).get("id")).longValue();

        exchange("/web/websuper/users/" + userId + "/phone?phone=13900005006",
                HttpMethod.PUT, null, systemToken);
        assertThat(userMapper.selectById(userId).getPhone()).isEqualTo("13900005006");

        Map<String, Object> duplicate = exchange("/web/websuper/users/" + userId + "/phone?phone=13800004001",
                HttpMethod.PUT, null, systemToken);
        assertThat(duplicate.get("code")).isEqualTo(400);
    }

    @Test
    void currentUserReturnsSwitchableRoles() {
        Map<String, Object> me = data(exchange("/auth/me", HttpMethod.GET, null, systemToken));
        assertThat(me.get("phone")).isEqualTo("13800004001");
        assertThat((List<?>) me.get("roles")).hasSize(1);
        assertThat(((Map<?, ?>) ((List<?>) me.get("roles")).get(0)).get("code")).isEqualTo(1);
    }
    @Test
    void inactiveRoleCanBeRemovedEvenWithSingleActiveRole() {
        Long userId = ((Number) data(exchange("/web/websuper/users", HttpMethod.POST,
                Map.of("phone", "13900005007", "name", "角色停用", "role", 3), systemToken)).get("id")).longValue();
        exchange("/web/websuper/users/" + userId + "/roles?role=5", HttpMethod.POST, null, systemToken);

        UserRole teacher = userRoleMapper.selectOne(new LambdaQueryWrapper<UserRole>()
                .eq(UserRole::getUserId, userId)
                .eq(UserRole::getRole, RoleEnum.TEACHER));
        teacher.setStatus(0);
        userRoleMapper.updateById(teacher);

        Map<String, Object> removed = exchange("/web/websuper/users/" + userId + "/roles/5",
                HttpMethod.DELETE, null, systemToken);
        assertThat(removed.get("code")).isEqualTo(200);
        assertThat(userRoleMapper.selectCount(new LambdaQueryWrapper<UserRole>()
                .eq(UserRole::getUserId, userId))).isEqualTo(1L);
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
