package com.visitor.workflow;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.visitor.common.enums.PersonTypeEnum;
import com.visitor.common.enums.RoleEnum;
import com.visitor.domain.entity.ApprovalFlow;
import com.visitor.domain.entity.ApprovalInstance;
import com.visitor.domain.entity.ApprovalRule;
import com.visitor.domain.entity.ApprovalTask;
import com.visitor.domain.entity.SchoolClass;
import com.visitor.domain.entity.Staff;
import com.visitor.domain.entity.User;
import com.visitor.domain.entity.UserRole;
import com.visitor.security.JwtUtil;
import com.visitor.server.mapper.ApprovalFlowMapper;
import com.visitor.server.mapper.ApprovalInstanceMapper;
import com.visitor.server.mapper.ApprovalRuleMapper;
import com.visitor.server.mapper.ApprovalTaskMapper;
import com.visitor.server.mapper.SchoolClassMapper;
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
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
class ApprovalModuleIntegrationTest {

    @LocalServerPort
    private int port;

    @Autowired
    private TestRestTemplate rest;

    @Autowired
    private ApprovalEngine approvalEngine;

    @Autowired
    private ApprovalFlowMapper flowMapper;

    @Autowired
    private ApprovalRuleMapper ruleMapper;

    @Autowired
    private ApprovalInstanceMapper instanceMapper;

    @Autowired
    private ApprovalTaskMapper taskMapper;

    @Autowired
    private UserMapper userMapper;

    @Autowired
    private UserRoleMapper userRoleMapper;

    @Autowired
    private StaffMapper staffMapper;

    @Autowired
    private SchoolClassMapper classMapper;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private JwtUtil jwtUtil;

    private Long teacherUserId;
    private Long studentUserId;
    private Long counselorUserId;
    private Long teacherStaffId;
    private Long studentStaffId;
    private String teacherToken;
    private String studentToken;
    private String counselorToken;
    private String adminToken;
    private String otherSchoolAdminToken;
    private String superToken;

    @BeforeEach
    void setUp() {
        taskMapper.delete(null);
        instanceMapper.delete(null);
        flowMapper.delete(null);
        ruleMapper.delete(null);
        staffMapper.delete(null);
        classMapper.delete(null);
        userRoleMapper.delete(null);
        userMapper.delete(null);

        teacherUserId = createUser("13800001001", RoleEnum.TEACHER, 1L);
        studentUserId = createUser("13800001002", RoleEnum.STUDENT, 1L);
        counselorUserId = createUser("13800001003", RoleEnum.TEACHER, 1L);
        Long adminUserId = createUser("13800001004", RoleEnum.ADMIN, 1L);
        Long otherAdminUserId = createUser("13800001005", RoleEnum.ADMIN, 2L);
        Long superUserId = createUser("13800001006", RoleEnum.SUPER, null);

        teacherStaffId = createStaff("张老师", "13800001001", PersonTypeEnum.TEACHER, null, null);
        counselorUserId = counselorUserId;
        Long counselorStaffId = createStaff("辅导员", "13800001003", PersonTypeEnum.TEACHER, null, null);
        SchoolClass schoolClass = createClass("计算机2201", counselorStaffId);
        studentStaffId = createStaff("学生甲", "13800001002", PersonTypeEnum.STUDENT, schoolClass.getId(), counselorStaffId);

        teacherToken = token(teacherUserId, RoleEnum.TEACHER, 1L);
        studentToken = token(studentUserId, RoleEnum.STUDENT, 1L);
        counselorToken = token(counselorUserId, RoleEnum.TEACHER, 1L);
        adminToken = token(adminUserId, RoleEnum.ADMIN, 1L);
        otherSchoolAdminToken = token(otherAdminUserId, RoleEnum.ADMIN, 2L);
        superToken = token(superUserId, RoleEnum.SUPER, null);

        insertFlow("VISIT_DEFAULT", "默认访客流程",
                "{\"code\":\"VISIT_DEFAULT\",\"name\":\"默认访客流程\",\"timeoutHours\":24,"
                        + "\"nodes\":[{\"seq\":1,\"name\":\"被访人审批\",\"assignee\":\"TARGET\"}]}");
        insertFlow("VISIT_STUDENT", "学生访客流程",
                "{\"code\":\"VISIT_STUDENT\",\"name\":\"学生访客流程\",\"timeoutHours\":24,"
                        + "\"nodes\":[{\"seq\":1,\"name\":\"学生审批\",\"assignee\":\"TARGET\"},"
                        + "{\"seq\":2,\"name\":\"辅导员审批\",\"assignee\":\"COUNSELOR\"}]}");

        insertRule("TEACHER", "VISIT_DEFAULT");
        insertRule("STUDENT", "VISIT_STUDENT");
    }

    @Test
    void designerCanSaveValidatePublishAndSeeVersions() {
        String draft = """
                {"code":"VISIT_DEFAULT","name":"默认访客流程 v2","timeoutHours":12,
                 "nodes":[{"seq":1,"name":"被访人审批","assignee":"TARGET"}]}
                """;

        Map<String, Object> saved = data(exchange("/web/websuper/approval-flows/VISIT_DEFAULT/draft",
                HttpMethod.PUT, draft, superToken));
        assertThat(saved.get("version")).isEqualTo(2);
        assertThat(saved.get("status")).isEqualTo(0);

        assertThat((List<?>) exchange("/web/websuper/approval-flows/VISIT_DEFAULT/validate",
                HttpMethod.POST, null, superToken).get("data")).isEmpty();

        Map<String, Object> published = data(exchange("/web/websuper/approval-flows/VISIT_DEFAULT/publish",
                HttpMethod.POST, null, superToken));
        assertThat(published.get("status")).isEqualTo(1);

        List<?> versions = (List<?>) exchange("/web/websuper/approval-flows/VISIT_DEFAULT/versions",
                HttpMethod.GET, null, superToken).get("data");
        assertThat(versions).hasSize(2);
    }

    @Test
    void invalidFlowCannotBePublished() {
        String invalid = """
                {"code":"VISIT_DEFAULT","name":"坏流程","nodes":[]}
                """;
        exchange("/web/websuper/approval-flows/VISIT_DEFAULT/draft", HttpMethod.PUT, invalid, superToken);

        Map<String, Object> validate = exchange("/web/websuper/approval-flows/VISIT_DEFAULT/validate",
                HttpMethod.POST, null, superToken);
        assertThat((List<?>) validate.get("data")).isNotEmpty();

        Map<String, Object> publish = exchange("/web/websuper/approval-flows/VISIT_DEFAULT/publish",
                HttpMethod.POST, null, superToken);
        assertThat(publish.get("code")).isEqualTo(400);
    }

    @Test
    void teacherFlowCreatesTaskAndApproves() {
        ApprovalInstance instance = start(1L, "TEACHER", teacherStaffId);
        ApprovalTask task = firstTask(instance.getId());

        assertThat(task.getAssigneeId()).isEqualTo(teacherUserId);
        assertThat(task.getStatus()).isEqualTo(ApprovalStatuses.PENDING);

        Map<String, Object> result = exchange("/web/approval/tasks/" + task.getId() + "/approve?comment=ok",
                HttpMethod.POST, null, teacherToken);

        assertThat(result.get("code")).isEqualTo(200);
        assertThat(instanceMapper.selectById(instance.getId()).getStatus()).isEqualTo(ApprovalStatuses.APPROVED);
    }

    @Test
    void studentFlowNeedsTwoApprovals() {
        ApprovalInstance instance = start(2L, "STUDENT", studentStaffId);
        ApprovalTask first = firstTask(instance.getId());
        assertThat(first.getAssigneeId()).isEqualTo(studentUserId);

        exchange("/web/approval/tasks/" + first.getId() + "/approve", HttpMethod.POST, null, studentToken);

        ApprovalTask second = taskMapper.selectOne(new LambdaQueryWrapper<ApprovalTask>()
                .eq(ApprovalTask::getInstanceId, instance.getId())
                .eq(ApprovalTask::getSeq, 2));
        assertThat(second.getAssigneeId()).isEqualTo(counselorUserId);
        assertThat(instanceMapper.selectById(instance.getId()).getStatus()).isEqualTo(ApprovalStatuses.RUNNING);

        exchange("/web/approval/tasks/" + second.getId() + "/approve", HttpMethod.POST, null, counselorToken);

        assertThat(instanceMapper.selectById(instance.getId()).getStatus()).isEqualTo(ApprovalStatuses.APPROVED);
    }

    @Test
    void nonAssigneeCannotApprove() {
        ApprovalInstance instance = start(3L, "TEACHER", teacherStaffId);
        ApprovalTask task = firstTask(instance.getId());

        Map<String, Object> result = exchange("/web/approval/tasks/" + task.getId() + "/approve",
                HttpMethod.POST, null, counselorToken);

        assertThat(result.get("code")).isEqualTo(403);
        assertThat(taskMapper.selectById(task.getId()).getStatus()).isEqualTo(ApprovalStatuses.PENDING);
    }

    @Test
    void rejectEndsInstanceAndSkipsRemainingTasks() {
        ApprovalInstance instance = start(4L, "STUDENT", studentStaffId);
        ApprovalTask task = firstTask(instance.getId());

        exchange("/web/approval/tasks/" + task.getId() + "/reject?comment=no", HttpMethod.POST, null, studentToken);

        assertThat(instanceMapper.selectById(instance.getId()).getStatus()).isEqualTo(ApprovalStatuses.REJECTED);
    }

    @Test
    void adminCanEmergencyApproveButOtherSchoolCannot() {
        ApprovalInstance instance = start(5L, "TEACHER", teacherStaffId);
        ApprovalTask task = firstTask(instance.getId());

        Map<String, Object> forbidden = exchange(
                "/web/approval/tasks/" + task.getId() + "/approve?emergency=true", HttpMethod.POST, null, otherSchoolAdminToken);
        assertThat(forbidden.get("code")).isEqualTo(403);

        Map<String, Object> ok = exchange(
                "/web/approval/tasks/" + task.getId() + "/approve?emergency=true", HttpMethod.POST, null, adminToken);
        assertThat(ok.get("code")).isEqualTo(200);
        assertThat(taskMapper.selectById(task.getId()).getEmergency()).isEqualTo(1);
    }

    @Test
    void overdueTaskIsExpiredAndInstanceRejected() {
        ApprovalInstance instance = start(6L, "TEACHER", teacherStaffId);
        ApprovalTask task = firstTask(instance.getId());
        task.setDueAt(LocalDateTime.now().minusHours(1));
        taskMapper.updateById(task);

        int expired = approvalEngine.expireOverdueTasks();

        assertThat(expired).isGreaterThanOrEqualTo(1);
        assertThat(taskMapper.selectById(task.getId()).getStatus()).isEqualTo(ApprovalStatuses.EXPIRED);
        assertThat(instanceMapper.selectById(instance.getId()).getStatus()).isEqualTo(ApprovalStatuses.REJECTED);
    }

    @Test
    void batchApproveHandlesMultipleTasks() {
        ApprovalInstance first = start(7L, "TEACHER", teacherStaffId);
        ApprovalInstance second = start(8L, "TEACHER", teacherStaffId);
        List<Long> ids = List.of(firstTask(first.getId()).getId(), firstTask(second.getId()).getId());

        Map<String, Object> body = Map.of("taskIds", ids, "comment", "批量通过");
        Map<String, Object> result = exchange("/web/approval/tasks/batch-approve", HttpMethod.POST, body, teacherToken);

        assertThat((List<?>) result.get("data")).hasSize(2);
        assertThat(instanceMapper.selectById(first.getId()).getStatus()).isEqualTo(ApprovalStatuses.APPROVED);
        assertThat(instanceMapper.selectById(second.getId()).getStatus()).isEqualTo(ApprovalStatuses.APPROVED);
    }

    @Test
    void adminCannotAccessFlowDesigner() {
        Map<String, Object> result = exchange("/web/websuper/approval-flows", HttpMethod.GET, null, adminToken);

        assertThat(result.get("code")).isEqualTo(403);
    }

    @Test
    void instancesPageAndCancelWork() {
        ApprovalInstance instance = start(9L, "TEACHER", teacherStaffId);
        Map<String, Object> page = data(exchange("/web/approval/instances?page=1&size=5",
                HttpMethod.GET, null, adminToken));
        assertThat(((Number) page.get("total")).intValue()).isGreaterThanOrEqualTo(1);

        Map<String, Object> cancel = exchange("/web/approval/instances/" + instance.getId() + "/cancel?reason=test",
                HttpMethod.POST, null, adminToken);
        assertThat(cancel.get("code")).isEqualTo(200);
        assertThat(instanceMapper.selectById(instance.getId()).getStatus()).isEqualTo("CANCELLED");
    }

    @Test
    void flowDetailVersionsAndOfflineWork() {
        Map<String, Object> detail = data(exchange("/web/websuper/approval-flows/VISIT_DEFAULT?version=1",
                HttpMethod.GET, null, superToken));
        assertThat(detail.get("flowCode")).isEqualTo("VISIT_DEFAULT");

        List<?> versions = (List<?>) exchange("/web/websuper/approval-flows/VISIT_DEFAULT/versions",
                HttpMethod.GET, null, superToken).get("data");
        assertThat(versions).isNotEmpty();

        Map<String, Object> offline = exchange("/web/websuper/approval-flows/VISIT_DEFAULT/offline",
                HttpMethod.POST, null, superToken);
        assertThat(offline.get("code")).isEqualTo(200);
    }
    private ApprovalInstance start(Long bizId, String targetType, Long targetId) {
        return approvalEngine.start(new ApprovalEngine.StartCommand(
                "VISITOR_RECORD", bizId, 1L, studentUserId, "NORMAL", targetType, targetId));
    }

    private ApprovalTask firstTask(Long instanceId) {
        return taskMapper.selectOne(new LambdaQueryWrapper<ApprovalTask>()
                .eq(ApprovalTask::getInstanceId, instanceId)
                .eq(ApprovalTask::getSeq, 1));
    }

    private Long createUser(String phone, RoleEnum role, Long schoolId) {
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
        userRole.setSchoolId(schoolId);
        userRole.setIsDefault(1);
        userRole.setStatus(1);
        userRole.setCreatedAt(LocalDateTime.now());
        userRole.setUpdatedAt(LocalDateTime.now());
        userRoleMapper.insert(userRole);
        return user.getId();
    }

    private Long createStaff(String name, String phone, PersonTypeEnum type, Long classId, Long counselorId) {
        Staff staff = new Staff();
        staff.setSchoolId(1L);
        staff.setName(name);
        staff.setPhone(phone);
        staff.setPersonType(type);
        staff.setClassId(classId);
        staff.setCounselorId(counselorId);
        staff.setStatus(1);
        staff.setSyncSource("LOCAL");
        staff.setCreatedAt(LocalDateTime.now());
        staff.setUpdatedAt(LocalDateTime.now());
        staffMapper.insert(staff);
        return staff.getId();
    }

    private SchoolClass createClass(String name, Long counselorId) {
        SchoolClass schoolClass = new SchoolClass();
        schoolClass.setSchoolId(1L);
        schoolClass.setName(name);
        schoolClass.setCounselorId(counselorId);
        schoolClass.setStatus(1);
        schoolClass.setCreatedAt(LocalDateTime.now());
        schoolClass.setUpdatedAt(LocalDateTime.now());
        classMapper.insert(schoolClass);
        return schoolClass;
    }

    private void insertFlow(String code, String name, String json) {
        ApprovalFlow flow = new ApprovalFlow();
        flow.setFlowCode(code);
        flow.setName(name);
        flow.setVersion(1);
        flow.setStatus(1);
        flow.setDefinitionJson(json);
        flow.setCreatedAt(LocalDateTime.now());
        flow.setUpdatedAt(LocalDateTime.now());
        flowMapper.insert(flow);
    }

    private void insertRule(String targetType, String flowCode) {
        ApprovalRule rule = new ApprovalRule();
        rule.setSchoolId(null);
        rule.setVisitorType(null);
        rule.setTargetType(targetType);
        rule.setFlowCode(flowCode);
        rule.setPriority(10);
        rule.setStatus(1);
        rule.setCreatedAt(LocalDateTime.now());
        rule.setUpdatedAt(LocalDateTime.now());
        ruleMapper.insert(rule);
    }

    private String token(Long userId, RoleEnum role, Long schoolId) {
        return jwtUtil.generate(userId, "user-" + userId, List.of(role), role, schoolId);
    }

    private Map<String, Object> exchange(String path, HttpMethod method, Object payload, String token) {
        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(token);
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
