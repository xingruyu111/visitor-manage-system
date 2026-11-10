package com.visitor.org;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.visitor.common.enums.PersonTypeEnum;
import com.visitor.common.enums.RoleEnum;
import com.visitor.domain.entity.CounselorOrg;
import com.visitor.domain.entity.Org;
import com.visitor.domain.entity.Staff;
import com.visitor.domain.entity.User;
import com.visitor.domain.entity.UserRole;
import com.visitor.server.mapper.CounselorOrgMapper;
import com.visitor.server.mapper.OrgMapper;
import com.visitor.server.mapper.StaffMapper;
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
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
class CounselorControllerIntegrationTest {

    @LocalServerPort
    private int port;

    @Autowired
    private TestRestTemplate rest;
    @Autowired
    private CounselorOrgMapper counselorOrgMapper;
    @Autowired
    private StaffMapper staffMapper;
    @Autowired
    private OrgMapper orgMapper;
    @Autowired
    private UserMapper userMapper;
    @Autowired
    private UserRoleMapper userRoleMapper;
    @Autowired
    private PasswordEncoder passwordEncoder;

    private String token;
    private Long rootId;
    private Long childId;
    private Long counselorId;
    private Long merchantId;

    @BeforeEach
    void setUp() {
        counselorOrgMapper.delete(null);
        userRoleMapper.delete(null);
        userMapper.delete(null);
        staffMapper.delete(null);
        orgMapper.delete(null);

        rootId = org("人工智能学院", null, null, 1);
        childId = org("24软件1", rootId, String.valueOf(rootId), 2);
        counselorId = staff("张老师", "T4001", "13900004011", PersonTypeEnum.TEACHER, rootId);
        staff("学生甲", "S4001", "13900004012", PersonTypeEnum.STUDENT, childId);
        merchantId = staff("商户甲", "M4001", "13900004013", PersonTypeEnum.MERCHANT, rootId);

        User admin = new User();
        admin.setPhone("13900004001");
        admin.setName("测试管理员");
        admin.setPassword(passwordEncoder.encode("admin123"));
        admin.setStatus(1);
        admin.setCreatedAt(LocalDateTime.now());
        admin.setUpdatedAt(LocalDateTime.now());
        userMapper.insert(admin);
        UserRole role = new UserRole();
        role.setUserId(admin.getId());
        role.setRole(RoleEnum.SYSTEM_ADMIN);
        role.setIsDefault(1);
        role.setStatus(1);
        role.setCreatedAt(LocalDateTime.now());
        role.setUpdatedAt(LocalDateTime.now());
        userRoleMapper.insert(role);

        ResponseEntity<Map> login = rest.postForEntity(url("/auth/login"),
                Map.of("phone", "13900004001", "password", "admin123"), Map.class);
        token = String.valueOf(((Map) login.getBody().get("data")).get("token"));
    }

    @Test
    void counselorCrudAndPreview() {
        Map<String, Object> created = data(post("/web/admin/counselors",
                Map.of("counselorStaffId", counselorId, "orgId", rootId)));
        assertThat(created.get("orgName")).isEqualTo("人工智能学院");
        assertThat(((Number) created.get("studentCount")).intValue()).isEqualTo(1);

        Map<String, Object> page = data(get("/web/admin/counselors?keyword=张老师&page=1&size=10"));
        assertThat(((Number) page.get("total")).intValue()).isEqualTo(1);

        java.util.List<?> candidates = (java.util.List<?>) get("/web/admin/counselors/candidates?keyword=张").get("data");
        assertThat(candidates).hasSize(1);

        Map<String, Object> preview = data(get("/web/admin/counselors/preview?orgId=" + childId));
        assertThat(((Number) preview.get("studentCount")).intValue()).isEqualTo(1);

        Long id = ((Number) created.get("id")).longValue();
        assertThat(delete("/web/admin/orgs/" + rootId).get("code")).isEqualTo(400);
        Map<String, Object> updated = data(put("/web/admin/counselors/" + id + "?orgId=" + childId));
        assertThat(((Number) updated.get("orgId")).longValue()).isEqualTo(childId);

        assertThat(delete("/web/admin/counselors/" + id).get("code")).isEqualTo(200);
        assertThat(counselorOrgMapper.selectCount(null)).isZero();
    }

    @Test
    void invalidInputsAndDuplicateOrg() {
        post("/web/admin/counselors", Map.of("counselorStaffId", counselorId, "orgId", rootId));

        Map<String, Object> duplicate = post("/web/admin/counselors",
                Map.of("counselorStaffId", counselorId, "orgId", rootId));
        assertThat(duplicate.get("code")).isEqualTo(409);

        Map<String, Object> merchant = post("/web/admin/counselors",
                Map.of("counselorStaffId", merchantId, "orgId", childId));
        assertThat(merchant.get("code")).isEqualTo(400);

        Map<String, Object> missing = post("/web/admin/counselors", Map.of());
        assertThat(missing.get("code")).isEqualTo(400);

        Map<String, Object> badPreview = get("/web/admin/counselors/preview?orgId=999999");
        assertThat(badPreview.get("code")).isEqualTo(400);
    }

    @Test
    void updateToOccupiedOrgIsRejected() {
        Map<String, Object> first = data(post("/web/admin/counselors",
                Map.of("counselorStaffId", counselorId, "orgId", rootId)));
        post("/web/admin/counselors", Map.of("counselorStaffId", staff("李老师", "T4002",
                "13900004014", PersonTypeEnum.TEACHER, childId), "orgId", childId));

        Map<String, Object> conflict = put("/web/admin/counselors/" + first.get("id") + "?orgId=" + childId);
        assertThat(conflict.get("code")).isEqualTo(409);
    }

    private Map<String, Object> get(String path) {
        return rest.exchange(url(path), HttpMethod.GET, new HttpEntity<>(bearer()), Map.class).getBody();
    }

    private Map<String, Object> post(String path, Object body) {
        HttpHeaders headers = bearer();
        headers.setContentType(MediaType.APPLICATION_JSON);
        return rest.exchange(url(path), HttpMethod.POST, new HttpEntity<>(body, headers), Map.class).getBody();
    }

    private Map<String, Object> put(String path) {
        return rest.exchange(url(path), HttpMethod.PUT, new HttpEntity<>(bearer()), Map.class).getBody();
    }

    private Map<String, Object> delete(String path) {
        return rest.exchange(url(path), HttpMethod.DELETE, new HttpEntity<>(bearer()), Map.class).getBody();
    }

    private HttpHeaders bearer() {
        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(token);
        return headers;
    }

    private Map<String, Object> data(Map<String, Object> body) {
        return (Map<String, Object>) body.get("data");
    }

    private String url(String path) {
        return "http://localhost:" + port + "/api" + path;
    }

    private Long org(String name, Long parentId, String ancestors, int level) {
        Org org = new Org();
        org.setName(name);
        org.setParentId(parentId);
        org.setAncestors(ancestors);
        org.setLevel(level);
        org.setStatus(1);
        org.setCreatedAt(LocalDateTime.now());
        org.setUpdatedAt(LocalDateTime.now());
        orgMapper.insert(org);
        return org.getId();
    }

    private Long staff(String name, String personNo, String phone, PersonTypeEnum type, Long orgId) {
        Staff staff = new Staff();
        staff.setSchoolId(1L);
        staff.setName(name);
        staff.setPersonNo(personNo);
        staff.setPhone(phone);
        staff.setPersonType(type);
        staff.setOrgId(orgId);
        staff.setStatus(1);
        staff.setIsDeleted(0);
        staff.setSyncSource("LOCAL");
        staff.setCreatedAt(LocalDateTime.now());
        staff.setUpdatedAt(LocalDateTime.now());
        staffMapper.insert(staff);
        return staff.getId();
    }
}
