package com.visitor.org;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.visitor.common.enums.RoleEnum;
import com.alibaba.excel.EasyExcel;
import com.visitor.domain.dto.StaffImportRow;
import com.visitor.domain.entity.Department;
import com.visitor.domain.entity.IntegrationConfig;
import com.visitor.domain.entity.SchoolClass;
import com.visitor.domain.entity.Staff;
import com.visitor.domain.entity.User;
import com.visitor.domain.entity.UserRole;
import com.visitor.security.JwtUtil;
import com.visitor.server.mapper.DepartmentMapper;
import com.visitor.server.mapper.IntegrationConfigMapper;
import com.visitor.server.mapper.SchoolClassMapper;
import com.visitor.server.mapper.StaffMapper;
import com.visitor.server.mapper.OrgMapper;
import com.visitor.server.mapper.UserMapper;
import com.visitor.server.mapper.UserRoleMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.http.HttpEntity;
import org.springframework.http.ResponseEntity;
import org.springframework.util.LinkedMultiValueMap;
import java.io.ByteArrayOutputStream;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
class OrgModuleIntegrationTest {

    @LocalServerPort
    private int port;

    @Autowired
    private TestRestTemplate rest;

    @Autowired
    private UserMapper userMapper;

    @Autowired
    private UserRoleMapper userRoleMapper;

    @Autowired
    private StaffMapper staffMapper;

    @Autowired
    private OrgMapper orgMapper;

    @Autowired
    private DepartmentMapper departmentMapper;

    @Autowired
    private SchoolClassMapper classMapper;

    @Autowired
    private IntegrationConfigMapper integrationConfigMapper;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private JwtUtil jwtUtil;

    private String adminToken;
    private String otherAdminToken;
    private String superToken;

    @BeforeEach
    void setUp() {
        orgMapper.delete(null);
        staffMapper.delete(null);
        classMapper.delete(null);
        departmentMapper.delete(null);
        integrationConfigMapper.delete(null);
        userRoleMapper.delete(null);
        userMapper.delete(null);

        adminToken = tokenFor("13600000001", RoleEnum.SECURITY_ADMIN, 1L);
        otherAdminToken = tokenFor("13600000002", RoleEnum.SECURITY_ADMIN, 2L);
        superToken = tokenFor("13600000003", RoleEnum.SYSTEM_ADMIN, null);

        IntegrationConfig config = new IntegrationConfig();
        config.setIntegrationKey("DATA_SYNC");
        config.setProtocol("MOCK");
        config.setEnabled(1);
        config.setStatus(1);
        config.setTimeoutMs(3000);
        config.setRetryTimes(0);
        config.setCreatedAt(LocalDateTime.now());
        config.setUpdatedAt(LocalDateTime.now());
        integrationConfigMapper.insert(config);
    }

    @Test
    void adminCanCreateDepartmentAndStaff() {
        Map<String, Object> dept = data(post("/departments", adminToken, Map.of("name", "信息工程学院")));
        assertThat(dept.get("name")).isEqualTo("信息工程学院");

        Long deptId = ((Number) dept.get("id")).longValue();
        Map<String, Object> staff = data(post("/staff", adminToken,
                Map.of("name", "张老师", "personNo", "T1001", "phone", "13911110001", "personType", 1, "deptId", deptId)));

        assertThat(staff.get("deptName")).isEqualTo("信息工程学院");

        Map<String, Object> page = data(get("/staff?page=1&size=10", adminToken));
        assertThat(((Number) page.get("total")).intValue()).isEqualTo(1);
    }

    @Test
    void syncCreatesOrgStaffAndClasses() {
        assertThat(data(post("/org-sync/org", adminToken, null)).get("created")).isEqualTo(2);
        assertThat(data(post("/org-sync/staff", adminToken, null)).get("created")).isEqualTo(3);
        assertThat(data(post("/org-sync/student", adminToken, null)).get("created")).isEqualTo(2);

        assertThat(departmentMapper.selectCount(null)).isEqualTo(2L);
        assertThat(staffMapper.selectCount(null)).isEqualTo(5L);
        assertThat(classMapper.selectCount(null)).isEqualTo(1L);
        assertThat(userMapper.selectCount(new LambdaQueryWrapper<User>().isNotNull(User::getPersonNo))).isEqualTo(5L);
    }

    @Test
    void syncIsIdempotentAndUpdatesInsteadOfDuplicating() {
        post("/org-sync/staff", adminToken, null);
        Map<String, Object> second = data(post("/org-sync/staff", adminToken, null));

        assertThat(second.get("created")).isEqualTo(0);
        assertThat(second.get("updated")).isEqualTo(3);
        assertThat(staffMapper.selectCount(null)).isEqualTo(3L);
    }

    @Test
    void syncDeactivatesMissingStaffAndRevokesRole() {
        Long userId = createUserWithRole("13900001999", RoleEnum.TEACHER);
        Staff legacy = new Staff();
        legacy.setSchoolId(1L);
        legacy.setName("离职老师");
        legacy.setPhone("13900001999");
        legacy.setPersonType(com.visitor.common.enums.PersonTypeEnum.TEACHER);
        legacy.setStatus(1);
        legacy.setSyncSource("SYNC");
        legacy.setExternalId("T999");
        legacy.setCreatedAt(LocalDateTime.now());
        legacy.setUpdatedAt(LocalDateTime.now());
        staffMapper.insert(legacy);

        Map<String, Object> result = data(post("/org-sync/staff", adminToken, null));

        assertThat(result.get("deactivated")).isEqualTo(1);
        assertThat(staffMapper.selectById(legacy.getId()).getStatus()).isZero();
        assertThat(userRoleMapper.selectList(new LambdaQueryWrapper<UserRole>()
                .eq(UserRole::getUserId, userId)).get(0).getStatus()).isZero();
    }

    @Test
    void deactivateStaffRevokesRoles() {
        Long userId = createUserWithRole("13900001888", RoleEnum.CANTEEN);
        Map<String, Object> staff = data(post("/staff", adminToken,
                Map.of("name", "食堂师傅", "personNo", "C1001", "phone", "13900001888", "personType", 4)));
        Long staffId = ((Number) staff.get("id")).longValue();

        assertThat(delete("/staff/" + staffId, adminToken).get("code")).isEqualTo(200);

        assertThat(staffMapper.selectById(staffId).getStatus()).isZero();
        assertThat(userRoleMapper.selectList(new LambdaQueryWrapper<UserRole>()
                .eq(UserRole::getUserId, userId)).get(0).getStatus()).isZero();
    }

    @Test
    void singleSchoolMeansAllAdminsSeeSameData() {
        post("/staff", adminToken, Map.of("name", "本校老师", "personNo", "T1002", "phone", "13911110002", "personType", 1));

        Map<String, Object> otherPage = data(get("/staff?page=1&size=10", otherAdminToken));

        assertThat(((Number) otherPage.get("total")).intValue()).isEqualTo(1);
    }

    @Test
    void systemAdminQueriesWithoutSchoolId() {
        Map<String, Object> body = get("/departments", superToken);

        assertThat(body.get("code")).isEqualTo(200);
    }

    @Test
    void syncFailsWhenProviderNotConfigured() {
        IntegrationConfig config = integrationConfigMapper.selectOne(
                new LambdaQueryWrapper<IntegrationConfig>().eq(IntegrationConfig::getIntegrationKey, "DATA_SYNC"));
        config.setEnabled(0);
        integrationConfigMapper.updateById(config);

        Map<String, Object> body = post("/org-sync/org", adminToken, null);

        assertThat(body.get("code")).isEqualTo(400);
        assertThat(body.get("message")).asString().contains("数据中台未配置");
    }

    @Test
    void departmentTreeReturnsNestedStructure() {
        Map<String, Object> root = data(post("/departments", adminToken, Map.of("name", "信息工程学院")));
        Long rootId = ((Number) root.get("id")).longValue();
        post("/departments", adminToken, Map.of("name", "计算机系", "parentId", rootId));

        List<?> roots = listData(get("/departments", adminToken));
        assertThat(roots).hasSize(1);
        Map<?, ?> rootNode = (Map<?, ?>) roots.get(0);
        assertThat(rootNode.get("level")).isEqualTo(1);
        List<?> children = (List<?>) rootNode.get("children");
        assertThat(children).hasSize(1);
        assertThat(((Map<?, ?>) children.get(0)).get("level")).isEqualTo(2);
    }

    @Test
    void cannotDeleteDepartmentWithChildren() {
        Map<String, Object> root = data(post("/departments", adminToken, Map.of("name", "信息工程学院")));
        Long rootId = ((Number) root.get("id")).longValue();
        post("/departments", adminToken, Map.of("name", "计算机系", "parentId", rootId));

        Map<String, Object> body = delete("/departments/" + rootId, adminToken);

        assertThat(body.get("code")).isEqualTo(400);
        assertThat(body.get("message")).asString().contains("下级部门");
    }

    @Test
    void cannotDeleteDepartmentWithStaff() {
        Map<String, Object> dept = data(post("/departments", adminToken, Map.of("name", "后勤处")));
        Long deptId = ((Number) dept.get("id")).longValue();
        post("/staff", adminToken, Map.of("name", "后勤老师", "personNo", "T1005", "phone", "13911110006", "personType", 1, "deptId", deptId));

        Map<String, Object> body = delete("/departments/" + deptId, adminToken);

        assertThat(body.get("code")).isEqualTo(400);
        assertThat(body.get("message")).asString().contains("还有人员");
    }

    @Test
    void updateDepartmentName() {
        Map<String, Object> dept = data(post("/departments", adminToken, Map.of("name", "旧名称")));
        Long deptId = ((Number) dept.get("id")).longValue();

        Map<String, Object> updated = data(put("/departments/" + deptId, adminToken,
                Map.of("name", "新名称", "sortOrder", 5)));

        assertThat(updated.get("name")).isEqualTo("新名称");
        assertThat(updated.get("sortOrder")).isEqualTo(5);
    }

    @Test
    void classCrudAndDuplicateNameRejected() {
        Map<String, Object> created = data(post("/classes", adminToken,
                Map.of("name", "计算机2201", "grade", "2022")));
        Long classId = ((Number) created.get("id")).longValue();

        assertThat(listData(get("/classes", adminToken))).hasSize(1);

        Map<String, Object> updated = data(put("/classes/" + classId, adminToken,
                Map.of("name", "计算机2201", "grade", "2023")));
        assertThat(updated.get("grade")).isEqualTo("2023");

        Map<String, Object> duplicate = post("/classes", adminToken,
                Map.of("name", "计算机2201", "grade", "2023"));
        assertThat(duplicate.get("code")).isEqualTo(400);
    }

    @Test
    void staffPhoneMustBeUniqueWithinSchool() {
        post("/staff", adminToken, Map.of("name", "张老师", "personNo", "T1003", "phone", "13911110003", "personType", 1));

        Map<String, Object> duplicate = post("/staff", adminToken,
                Map.of("name", "李老师", "personNo", "T1004", "phone", "13911110003", "personType", 1));

        assertThat(duplicate.get("code")).isEqualTo(400);
        assertThat(duplicate.get("message")).asString().contains("手机号已存在");
    }

    @Test
    void staffCanBeFilteredByPersonType() {
        post("/staff", adminToken, Map.of("name", "张老师", "personNo", "T1006", "phone", "13911110004", "personType", 1));
        post("/staff", adminToken, Map.of("name", "学生甲", "personNo", "S1001", "phone", "13911110005", "personType", 2));

        Map<String, Object> page = data(get("/staff?page=1&size=10&personType=2", adminToken));

        assertThat(((Number) page.get("total")).intValue()).isEqualTo(1);
    }

    private Long createUserWithRole(String phone, RoleEnum role) {
        User user = new User();
        user.setPhone(phone);
        user.setName("测试用户");
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

    @Test
    void staffExcelImportCreatesAccountsAndReportsErrors() throws Exception {
        StaffImportRow ok = new StaffImportRow();
        ok.setPersonNo("T-IMP-1");
        ok.setName("导入老师");
        ok.setPhone("13900007771");
        ok.setPersonType("教职工");
        StaffImportRow bad = new StaffImportRow();
        bad.setPersonNo("T-IMP-2");
        bad.setName("导入错误");
        bad.setPhone("13900007772");
        bad.setPersonType("不存在的类型");

        ByteArrayOutputStream out = new ByteArrayOutputStream();
        EasyExcel.write(out, StaffImportRow.class).sheet("人员").doWrite(List.of(ok, bad));

        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(adminToken);
        headers.setContentType(MediaType.MULTIPART_FORM_DATA);
        LinkedMultiValueMap<String, Object> body = new LinkedMultiValueMap<>();
        body.add("file", new ByteArrayResource(out.toByteArray()) {
            @Override
            public String getFilename() {
                return "staff.xlsx";
            }
        });
        ResponseEntity<Map> response = rest.exchange("http://localhost:" + port + "/api/web/admin/staff/import",
                HttpMethod.POST, new HttpEntity<>(body, headers), Map.class);

        Map<String, Object> data = (Map<String, Object>) response.getBody().get("data");
        assertThat(((Number) data.get("total")).intValue()).isEqualTo(2);
        assertThat(((Number) data.get("success")).intValue()).isEqualTo(1);
        assertThat(((Number) data.get("failed")).intValue()).isEqualTo(1);
        assertThat(userMapper.selectCount(new LambdaQueryWrapper<User>()
                .eq(User::getPersonNo, "T-IMP-1"))).isEqualTo(1L);
        assertThat(userRoleMapper.selectCount(new LambdaQueryWrapper<UserRole>()
                .eq(UserRole::getRole, RoleEnum.TEACHER))).isGreaterThanOrEqualTo(1L);
    }
    @Test
    void orgTreeAndStaffBinding() {
        Map<String, Object> college = data(post("/orgs?name=人工智能学院", adminToken, null));
        Long collegeId = ((Number) college.get("id")).longValue();
        Map<String, Object> grade = data(post("/orgs?name=24级&parentId=" + collegeId, adminToken, null));
        Long gradeId = ((Number) grade.get("id")).longValue();
        Map<String, Object> clazz = data(post("/orgs?name=24软件1&parentId=" + gradeId, adminToken, null));
        Long classId = ((Number) clazz.get("id")).longValue();

        java.util.List<?> tree = (java.util.List<?>) exchange("/orgs/tree", HttpMethod.GET, null, adminToken).get("data");
        assertThat(tree).hasSize(1);

        Map<String, Object> staff = data(post("/staff", adminToken,
                Map.of("name", "组织老师", "personNo", "T-ORG-1", "phone", "13900009991",
                        "personType", 1, "orgId", classId)));
        assertThat(staff.get("orgName")).isEqualTo("24软件1");

        Map<String, Object> byGrade = data(get("/staff?orgId=" + gradeId + "&page=1&size=10", adminToken));
        assertThat(((Number) byGrade.get("total")).intValue()).isEqualTo(1);
        Map<String, Object> byCollege = data(get("/staff?orgId=" + collegeId + "&page=1&size=10", adminToken));
        assertThat(((Number) byCollege.get("total")).intValue()).isEqualTo(1);

        Map<String, Object> blocked = exchange("/orgs/" + classId, HttpMethod.DELETE, null, adminToken);
        assertThat(blocked.get("code")).isEqualTo(400);
    }
    @Test
    void orgUpdateAndDeleteRules() {
        Map<String, Object> root = data(post("/orgs?name=信息学院", adminToken, null));
        Long rootId = ((Number) root.get("id")).longValue();
        Map<String, Object> child = data(post("/orgs?name=计算机系&parentId=" + rootId, adminToken, null));
        Long childId = ((Number) child.get("id")).longValue();
        Map<String, Object> leaf = data(post("/orgs?name=软件教研室&parentId=" + childId, adminToken, null));
        Long leafId = ((Number) leaf.get("id")).longValue();

        Map<String, Object> renamed = data(put("/orgs/" + rootId + "?name=信息工程学院&sortOrder=2", adminToken, null));
        assertThat(renamed.get("name")).isEqualTo("信息工程学院");

        Map<String, Object> cycle = exchange("/orgs/" + rootId + "?parentId=" + leafId,
                HttpMethod.PUT, null, adminToken);
        assertThat(cycle.get("code")).isEqualTo(400);

        Map<String, Object> hasChild = exchange("/orgs/" + rootId, HttpMethod.DELETE, null, adminToken);
        assertThat(hasChild.get("code")).isEqualTo(400);

        Map<String, Object> deleted = exchange("/orgs/" + leafId, HttpMethod.DELETE, null, adminToken);
        assertThat(deleted.get("code")).isEqualTo(200);
    }

    @Test
    void staffRejectsUnknownOrg() {
        Map<String, Object> result = post("/staff", adminToken,
                Map.of("name", "无组织老师", "personNo", "T-ORG-X", "phone", "13900009992",
                        "personType", 1, "orgId", 999999));
        assertThat(result.get("code")).isEqualTo(400);
    }

    @Test
    void staffImportReportsUnknownDeptAndClass() throws Exception {
        StaffImportRow deptBad = new StaffImportRow();
        deptBad.setPersonNo("T-IMP-3");
        deptBad.setName("部门错误");
        deptBad.setPhone("13900007773");
        deptBad.setPersonType("教职工");
        deptBad.setDeptName("不存在的部门");
        StaffImportRow classBad = new StaffImportRow();
        classBad.setPersonNo("T-IMP-4");
        classBad.setName("班级错误");
        classBad.setPhone("13900007774");
        classBad.setPersonType("学生");
        classBad.setClassName("不存在的班级");

        ByteArrayOutputStream out = new ByteArrayOutputStream();
        EasyExcel.write(out, StaffImportRow.class).sheet("人员").doWrite(List.of(deptBad, classBad));

        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(adminToken);
        headers.setContentType(MediaType.MULTIPART_FORM_DATA);
        LinkedMultiValueMap<String, Object> body = new LinkedMultiValueMap<>();
        body.add("file", new ByteArrayResource(out.toByteArray()) {
            @Override
            public String getFilename() {
                return "staff.xlsx";
            }
        });
        ResponseEntity<Map> response = rest.exchange("http://localhost:" + port + "/api/web/admin/staff/import",
                HttpMethod.POST, new HttpEntity<>(body, headers), Map.class);
        Map<String, Object> data = (Map<String, Object>) response.getBody().get("data");
        assertThat(((Number) data.get("failed")).intValue()).isEqualTo(2);
    }
    private String tokenFor(String phone, RoleEnum role, Long schoolId) {
        User user = new User();
        user.setPhone(phone);
        user.setName("管理员" + phone);
        user.setPassword(passwordEncoder.encode("pass1234"));
        user.setStatus(1);
        user.setCreatedAt(LocalDateTime.now());
        user.setUpdatedAt(LocalDateTime.now());
        userMapper.insert(user);

        UserRole userRole = new UserRole();
        userRole.setUserId(user.getId());
        userRole.setRole(role);
        userRole.setSchoolId(schoolId);
        userRole.setIsDefault(1);
        userRole.setStatus(1);
        userRole.setCreatedAt(LocalDateTime.now());
        userRole.setUpdatedAt(LocalDateTime.now());
        userRoleMapper.insert(userRole);

        return jwtUtil.generate(user.getId(), user.getName(),
                List.of(role), role, schoolId);
    }

    private Map<String, Object> get(String path, String token) {
        return exchange(path, HttpMethod.GET, null, token);
    }

    private Map<String, Object> post(String path, String token, Object payload) {
        return exchange(path, HttpMethod.POST, payload, token);
    }

    private Map<String, Object> delete(String path, String token) {
        return exchange(path, HttpMethod.DELETE, null, token);
    }

    private Map<String, Object> put(String path, String token, Object payload) {
        return exchange(path, HttpMethod.PUT, payload, token);
    }

    @SuppressWarnings("unchecked")
    private List<?> listData(Map<String, Object> body) {
        return (List<?>) body.get("data");
    }

    @SuppressWarnings("unchecked")
    private Map<String, Object> exchange(String path, HttpMethod method, Object payload, String token) {
        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(token);
        headers.setContentType(MediaType.APPLICATION_JSON);
        HttpEntity<Object> entity = new HttpEntity<>(payload, headers);
        ResponseEntity<Map> response = rest.exchange("http://localhost:" + port + "/api/web/admin" + path,
                method, entity, Map.class);
        return response.getBody();
    }

    @SuppressWarnings("unchecked")
    private Map<String, Object> data(Map<String, Object> body) {
        return (Map<String, Object>) body.get("data");
    }
}
