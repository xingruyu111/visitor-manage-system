package com.visitor.whitelist;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.visitor.common.enums.RoleEnum;
import com.visitor.domain.entity.AccessList;
import com.visitor.domain.entity.User;
import com.visitor.domain.entity.UserRole;
import com.visitor.domain.entity.VisitorPass;
import com.visitor.domain.entity.VisitorRecord;
import com.visitor.domain.dto.WhitelistImportRow;
import com.visitor.security.JwtUtil;
import com.visitor.security.UserContext;
import com.visitor.server.mapper.AccessListMapper;
import com.visitor.server.mapper.UserMapper;
import com.visitor.server.mapper.UserRoleMapper;
import com.visitor.server.mapper.VisitorPassMapper;
import com.visitor.server.mapper.VisitorRecordMapper;
import com.visitor.server.service.WhitelistService;
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
import org.springframework.test.context.ActiveProfiles;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
class WhitelistIntegrationTest {

    @LocalServerPort
    private int port;

    @Autowired
    private TestRestTemplate rest;

    @Autowired
    private JwtUtil jwtUtil;

    @Autowired
    private UserMapper userMapper;

    @Autowired
    private UserRoleMapper userRoleMapper;

    @Autowired
    private AccessListMapper accessListMapper;

    @Autowired
    private VisitorRecordMapper recordMapper;

    @Autowired
    private VisitorPassMapper passMapper;

    @Autowired
    private WhitelistService whitelistService;

    private Long adminUserId;
    private String adminToken;
    private String deptToken;

    @BeforeEach
    void setUp() {
        passMapper.delete(null);
        recordMapper.delete(null);
        accessListMapper.delete(null);
        userRoleMapper.delete(null);
        userMapper.delete(null);

        adminUserId = createUser("13800002001", RoleEnum.SECURITY_ADMIN);
        Long deptId = createUser("13800002002", RoleEnum.DEPT_OPERATOR);
        adminToken = token(adminUserId, RoleEnum.SECURITY_ADMIN);
        deptToken = token(deptId, RoleEnum.DEPT_OPERATOR);
    }

    @Test
    void securityAdminUploadTakesEffectAndIssuesUnlimitedPass() {
        LocalDateTime from = LocalDateTime.now().plusHours(1).withNano(0);
        LocalDateTime to = from.plusDays(30);
        Map<String, Object> created = exchange("/web/whitelist", HttpMethod.POST,
                body("张三", "13900002001", "粤B12345", from, to, "校企合作"), adminToken);
        assertThat(created.get("code")).isEqualTo(200);
        Map<?, ?> data = (Map<?, ?>) created.get("data");
        assertThat(data.get("status")).isEqualTo(1);
        assertThat(data.get("plateNumber")).isEqualTo("粤B12345");

        Long recordId = ((Number) data.get("visitorRecordId")).longValue();
        VisitorRecord record = recordMapper.selectById(recordId);
        assertThat(record.getStatus()).isEqualTo(1);
        assertThat(record.getPassMode()).isEqualTo("CAR");
        assertThat(record.getVisitPurpose()).isEqualTo("校企合作");
        assertThat(record.getExpectedTime()).isEqualTo(from);
        assertThat(record.getLeaveTime()).isEqualTo(to);

        VisitorPass pass = passMapper.selectOne(new LambdaQueryWrapper<VisitorPass>()
                .eq(VisitorPass::getRecordId, recordId));
        assertThat(pass).isNotNull();
        assertThat(pass.getMaxEntries()).isEqualTo(999999);
        assertThat(pass.getUsedEntries()).isZero();
    }

    @Test
    void deptUploadNeedsSecurityApprovalThenGeneratesRecord() {
        LocalDateTime from = LocalDateTime.now().plusHours(2).withNano(0);
        LocalDateTime to = from.plusDays(7);
        Map<String, Object> created = exchange("/web/whitelist", HttpMethod.POST,
                body("李四", "13900002002", "", from, to, "机关来访"), deptToken);
        assertThat(created.get("code")).isEqualTo(200);
        Map<?, ?> data = (Map<?, ?>) created.get("data");
        assertThat(data.get("status")).isEqualTo(0);
        assertThat(data.get("visitorRecordId")).isNull();

        Long id = ((Number) data.get("id")).longValue();
        assertThat(accessListMapper.selectById(id).getStatus()).isZero();
        assertThat(recordMapper.selectCount(null)).isZero();

        Map<String, Object> approved = exchange("/web/whitelist/" + id + "/approve",
                HttpMethod.POST, null, adminToken);
        assertThat(approved.get("code")).isEqualTo(200);
        Map<?, ?> after = (Map<?, ?>) approved.get("data");
        assertThat(after.get("status")).isEqualTo(1);
        Long recordId = ((Number) after.get("visitorRecordId")).longValue();
        VisitorRecord record = recordMapper.selectById(recordId);
        assertThat(record.getPassMode()).isEqualTo("WALK");
        assertThat(record.getStatus()).isEqualTo(1);
        assertThat(passMapper.selectCount(new LambdaQueryWrapper<VisitorPass>()
                .eq(VisitorPass::getRecordId, recordId))).isEqualTo(1);
    }

    @Test
    void duplicateActivePersonIsRejected() {
        LocalDateTime from = LocalDateTime.now().plusHours(1).withNano(0);
        LocalDateTime to = from.plusDays(1);
        exchange("/web/whitelist", HttpMethod.POST,
                body("王五", "13900002003", "", from, to, "测试"), adminToken);
        Map<String, Object> duplicate = exchange("/web/whitelist", HttpMethod.POST,
                body("王五", "13900002003", "", from, to, "测试"), adminToken);
        assertThat(duplicate.get("code")).isEqualTo(400);
    }

    @Test
    void departmentCannotApprove() {
        LocalDateTime from = LocalDateTime.now().plusHours(1).withNano(0);
        LocalDateTime to = from.plusDays(1);
        Map<String, Object> created = exchange("/web/whitelist", HttpMethod.POST,
                body("赵六", "13900002004", "", from, to, "测试"), deptToken);
        Long id = ((Number) ((Map<?, ?>) created.get("data")).get("id")).longValue();

        Map<String, Object> approve = exchange("/web/whitelist/" + id + "/approve",
                HttpMethod.POST, null, deptToken);
        assertThat(approve.get("code")).isEqualTo(403);
        Map<String, Object> disable = exchange("/web/whitelist/" + id + "/status?status=3",
                HttpMethod.PUT, null, deptToken);
        assertThat(disable.get("code")).isEqualTo(403);
    }

    @Test
    void rejectedPersonCanResubmitAndBeApproved() {
        LocalDateTime from = LocalDateTime.now().plusHours(1).withNano(0);
        LocalDateTime to = from.plusDays(2);
        Map<String, Object> created = exchange("/web/whitelist", HttpMethod.POST,
                body("钱七", "13900002005", "粤B88888", from, to, "第一次"), deptToken);
        Long id = ((Number) ((Map<?, ?>) created.get("data")).get("id")).longValue();

        Map<String, Object> rejected = exchange("/web/whitelist/" + id + "/reject?reason=材料不全",
                HttpMethod.POST, null, adminToken);
        assertThat(rejected.get("code")).isEqualTo(200);
        assertThat(((Map<?, ?>) rejected.get("data")).get("status")).isEqualTo(2);

        Map<String, Object> again = exchange("/web/whitelist", HttpMethod.POST,
                body("钱七", "13900002005", "粤B88888", from, to, "补充材料"), deptToken);
        assertThat(again.get("code")).isEqualTo(200);
        Map<?, ?> data = (Map<?, ?>) again.get("data");
        assertThat(((Number) data.get("id")).longValue()).isEqualTo(id);
        assertThat(data.get("status")).isEqualTo(0);

        Map<String, Object> approved = exchange("/web/whitelist/" + id + "/approve",
                HttpMethod.POST, null, adminToken);
        assertThat(approved.get("code")).isEqualTo(200);
        assertThat(((Map<?, ?>) approved.get("data")).get("status")).isEqualTo(1);
    }

    @Test
    void disableRevokesRecordAndEnableCreatesNewRecord() {
        LocalDateTime from = LocalDateTime.now().plusHours(1).withNano(0);
        LocalDateTime to = from.plusDays(3);
        Map<String, Object> created = exchange("/web/whitelist", HttpMethod.POST,
                body("孙八", "13900002006", "", from, to, "测试"), adminToken);
        Map<?, ?> data = (Map<?, ?>) created.get("data");
        Long id = ((Number) data.get("id")).longValue();
        Long firstRecordId = ((Number) data.get("visitorRecordId")).longValue();

        Map<String, Object> disabled = exchange("/web/whitelist/" + id + "/status?status=3",
                HttpMethod.PUT, null, adminToken);
        assertThat(disabled.get("code")).isEqualTo(200);
        assertThat(recordMapper.selectById(firstRecordId).getStatus()).isEqualTo(5);
        assertThat(passMapper.selectOne(new LambdaQueryWrapper<VisitorPass>()
                .eq(VisitorPass::getRecordId, firstRecordId)).getStatus()).isEqualTo(3);

        Map<String, Object> enabled = exchange("/web/whitelist/" + id + "/status?status=1",
                HttpMethod.PUT, null, adminToken);
        assertThat(enabled.get("code")).isEqualTo(200);
        Long secondRecordId = ((Number) ((Map<?, ?>) enabled.get("data")).get("visitorRecordId")).longValue();
        assertThat(secondRecordId).isNotEqualTo(firstRecordId);
        assertThat(recordMapper.selectById(secondRecordId).getStatus()).isEqualTo(1);
    }

    @Test
    void deleteRevokesRecordAndRemovesWhitelist() {
        LocalDateTime from = LocalDateTime.now().plusHours(1).withNano(0);
        LocalDateTime to = from.plusDays(1);
        Map<String, Object> created = exchange("/web/whitelist", HttpMethod.POST,
                body("周九", "13900002007", "", from, to, "测试"), adminToken);
        Map<?, ?> data = (Map<?, ?>) created.get("data");
        Long id = ((Number) data.get("id")).longValue();
        Long recordId = ((Number) data.get("visitorRecordId")).longValue();

        Map<String, Object> deleted = exchange("/web/whitelist/" + id, HttpMethod.DELETE, null, adminToken);
        assertThat(deleted.get("code")).isEqualTo(200);
        assertThat(accessListMapper.selectById(id)).isNull();
        assertThat(recordMapper.selectById(recordId).getStatus()).isEqualTo(5);
    }

    @Test
    void importRowsCreatesValidRowsAndReportsInvalid() {
        LocalDateTime from = LocalDateTime.now().plusHours(1).withNano(0);
        LocalDateTime to = from.plusDays(5);
        WhitelistImportRow valid = new WhitelistImportRow();
        valid.setPersonName("导入用户");
        valid.setPhone("13900002008");
        valid.setPlateNumber("");
        valid.setEffectiveFrom(from.toString().replace('T', ' '));
        valid.setEffectiveTo(to.toString().replace('T', ' '));
        valid.setReason("Excel导入");
        WhitelistImportRow invalid = new WhitelistImportRow();
        invalid.setPersonName("错误用户");
        invalid.setPhone("13900002009");
        invalid.setEffectiveFrom("not-a-time");
        invalid.setEffectiveTo(to.toString().replace('T', ' '));

        UserContext.set(adminUserId, "管理员", RoleEnum.SECURITY_ADMIN, 1L);
        try {
            Map<String, Object> result = whitelistService.importRows(List.of(valid, invalid));
            assertThat(result.get("success")).isEqualTo(1);
            assertThat(result.get("failed")).isEqualTo(1);
            assertThat(accessListMapper.selectCount(new LambdaQueryWrapper<AccessList>()
                    .eq(AccessList::getTargetValue, "13900002008"))).isEqualTo(1);
        } finally {
            UserContext.clear();
        }
    }

    @Test
    void keywordPageReturnsMatchingPerson() {
        LocalDateTime from = LocalDateTime.now().plusHours(1).withNano(0);
        LocalDateTime to = from.plusDays(1);
        exchange("/web/whitelist", HttpMethod.POST,
                body("关键字用户", "13900002010", "", from, to, "测试"), adminToken);
        Map<String, Object> page = exchange("/web/whitelist?keyword=关键字&page=1&size=20",
                HttpMethod.GET, null, adminToken);
        assertThat(page.get("code")).isEqualTo(200);
        assertThat(((Number) ((Map<?, ?>) page.get("data")).get("total")).intValue()).isEqualTo(1);
    }
    private Map<String, Object> body(String name, String phone, String plate,
                                     LocalDateTime from, LocalDateTime to, String reason) {
        return Map.of(
                "personName", name,
                "phone", phone,
                "plateNumber", plate,
                "effectiveFrom", from.toString().replace('T', ' '),
                "effectiveTo", to.toString().replace('T', ' '),
                "reason", reason
        );
    }

    private Long createUser(String phone, RoleEnum role) {
        User user = new User();
        user.setPhone(phone);
        user.setName(role.getDescription() + phone.substring(phone.length() - 4));
        user.setPassword("x");
        user.setStatus(1);
        user.setCreatedAt(LocalDateTime.now());
        user.setUpdatedAt(LocalDateTime.now());
        userMapper.insert(user);

        UserRole userRole = new UserRole();
        userRole.setUserId(user.getId());
        userRole.setRole(role);
        userRole.setSchoolId(1L);
        userRole.setIsDefault(1);
        userRole.setSource("MANUAL");
        userRole.setStatus(1);
        userRole.setCreatedAt(LocalDateTime.now());
        userRole.setUpdatedAt(LocalDateTime.now());
        userRoleMapper.insert(userRole);
        return user.getId();
    }

    private String token(Long userId, RoleEnum role) {
        return jwtUtil.generate(userId, role.getDescription(), List.of(role), role, 1L);
    }

    private Map<String, Object> exchange(String path, HttpMethod method, Object payload, String token) {
        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(token);
        headers.setContentType(MediaType.APPLICATION_JSON);
        ResponseEntity<Map> response = rest.exchange("http://localhost:" + port + "/api" + path, method,
                new HttpEntity<>(payload, headers), Map.class);
        return response.getBody();
    }
}
