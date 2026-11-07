package com.visitor.visit;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.visitor.common.enums.PersonTypeEnum;
import com.visitor.common.enums.RoleEnum;
import com.visitor.domain.entity.ApprovalFlow;
import com.visitor.domain.entity.ApprovalRule;
import com.visitor.domain.entity.ApprovalTask;
import com.visitor.domain.entity.Staff;
import com.visitor.domain.entity.User;
import com.visitor.domain.entity.UserRole;
import com.visitor.domain.entity.VisitorPass;
import com.visitor.domain.entity.VisitorRecord;
import com.visitor.security.JwtUtil;
import com.visitor.server.mapper.ApprovalFlowMapper;
import com.visitor.server.mapper.ApprovalInstanceMapper;
import com.visitor.server.mapper.ApprovalRuleMapper;
import com.visitor.server.mapper.ApprovalTaskMapper;
import com.visitor.server.mapper.StaffMapper;
import com.visitor.server.mapper.UserMapper;
import com.visitor.server.mapper.UserRoleMapper;
import com.visitor.server.mapper.VisitorPassMapper;
import com.visitor.server.mapper.VisitorRecordMapper;
import com.visitor.server.service.VisitorService;
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
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
class VisitorModuleIntegrationTest {

    private static final DateTimeFormatter FORMAT = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    @LocalServerPort
    private int port;

    @Autowired
    private TestRestTemplate rest;

    @Autowired
    private VisitorService visitorService;

    @Autowired
    private UserMapper userMapper;

    @Autowired
    private UserRoleMapper userRoleMapper;

    @Autowired
    private StaffMapper staffMapper;

    @Autowired
    private VisitorRecordMapper recordMapper;

    @Autowired
    private VisitorPassMapper passMapper;

    @Autowired
    private ApprovalTaskMapper taskMapper;

    @Autowired
    private ApprovalInstanceMapper instanceMapper;

    @Autowired
    private ApprovalFlowMapper flowMapper;

    @Autowired
    private ApprovalRuleMapper ruleMapper;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private JwtUtil jwtUtil;

    private Long visitorUserId;
    private Long teacherStaffId;
    private Long teacherUserId;
    private String visitorToken;
    private String otherVisitorToken;
    private String teacherToken;
    private String adminToken;
    private String otherAdminToken;

    @BeforeEach
    void setUp() {
        passMapper.delete(null);
        recordMapper.delete(null);
        taskMapper.delete(null);
        instanceMapper.delete(null);
        flowMapper.delete(null);
        ruleMapper.delete(null);
        staffMapper.delete(null);
        userRoleMapper.delete(null);
        userMapper.delete(null);

        visitorUserId = createUser("13800003001", RoleEnum.VISITOR, 1L);
        Long otherVisitorId = createUser("13800003002", RoleEnum.VISITOR, 1L);
        teacherUserId = createUser("13800003003", RoleEnum.TEACHER, 1L);
        Long adminId = createUser("13800003004", RoleEnum.ADMIN, 1L);
        Long otherAdminId = createUser("13800003005", RoleEnum.ADMIN, 2L);

        teacherStaffId = createStaff("张老师", "13800003003", PersonTypeEnum.TEACHER);

        visitorToken = token(visitorUserId, "13800003001", RoleEnum.VISITOR, null);
        otherVisitorToken = token(otherVisitorId, "13800003002", RoleEnum.VISITOR, null);
        teacherToken = token(teacherUserId, "13800003003", RoleEnum.TEACHER, 1L);
        adminToken = token(adminId, "13800003004", RoleEnum.ADMIN, 1L);
        otherAdminToken = token(otherAdminId, "13800003005", RoleEnum.ADMIN, 2L);

        insertFlow();
        insertRule();
    }

    @Test
    void applyCreatesPendingRecordAndTask() {
        Map<String, Object> result = apply(visitorToken, "13800003001", 1);

        assertThat(result.get("code")).isEqualTo(200);
        Long recordId = number(data(result).get("recordId"));
        Long instanceId = number(data(result).get("approvalInstanceId"));

        assertThat(recordMapper.selectById(recordId).getStatus()).isZero();
        ApprovalTask task = taskMapper.selectOne(new LambdaQueryWrapper<ApprovalTask>()
                .eq(ApprovalTask::getInstanceId, instanceId));
        assertThat(task.getAssigneeId()).isEqualTo(teacherUserId);
    }

    @Test
    void approvedApplicationIssuesPass() {
        Map<String, Object> applied = apply(visitorToken, "13800003001", 1);
        Long recordId = number(data(applied).get("recordId"));
        Long instanceId = number(data(applied).get("approvalInstanceId"));
        ApprovalTask task = taskMapper.selectOne(new LambdaQueryWrapper<ApprovalTask>()
                .eq(ApprovalTask::getInstanceId, instanceId));

        exchange("/web/approval/tasks/" + task.getId() + "/approve?comment=欢迎",
                HttpMethod.POST, null, teacherToken);

        assertThat(recordMapper.selectById(recordId).getStatus()).isEqualTo(1);
        VisitorPass pass = passMapper.selectOne(new LambdaQueryWrapper<VisitorPass>()
                .eq(VisitorPass::getRecordId, recordId));
        assertThat(pass).isNotNull();
        assertThat(pass.getQrToken()).isNotBlank();
        assertThat(pass.getMaxEntries()).isEqualTo(1);

        Map<String, Object> passBody = data(exchange("/web/visitor/records/" + recordId + "/pass",
                HttpMethod.GET, null, visitorToken));
        assertThat(passBody.get("qrToken")).isEqualTo(pass.getQrToken());
    }

    @Test
    void rejectedApplicationHasNoPass() {
        Map<String, Object> applied = apply(visitorToken, "13800003001", 1);
        Long recordId = number(data(applied).get("recordId"));
        Long instanceId = number(data(applied).get("approvalInstanceId"));
        ApprovalTask task = taskMapper.selectOne(new LambdaQueryWrapper<ApprovalTask>()
                .eq(ApprovalTask::getInstanceId, instanceId));

        exchange("/web/approval/tasks/" + task.getId() + "/reject?comment=不接待",
                HttpMethod.POST, null, teacherToken);

        assertThat(recordMapper.selectById(recordId).getStatus()).isEqualTo(2);
        assertThat(passMapper.selectCount(null)).isZero();
    }

    @Test
    void duplicateOverlappingApplicationRejected() {
        apply(visitorToken, "13800003001", 1);

        Map<String, Object> second = apply(visitorToken, "13800003001", 1);

        assertThat(second.get("code")).isEqualTo(400);
        assertThat(second.get("message")).asString().contains("已有预约");
    }

    @Test
    void visitorCannotReadOthersRecord() {
        Map<String, Object> applied = apply(visitorToken, "13800003001", 1);
        Long recordId = number(data(applied).get("recordId"));

        Map<String, Object> detail = exchange("/web/visitor/records/" + recordId,
                HttpMethod.GET, null, otherVisitorToken);

        assertThat(detail.get("code")).isEqualTo(404);
    }

    @Test
    void cancelRevokesPass() {
        Map<String, Object> applied = apply(visitorToken, "13800003001", 1);
        Long recordId = number(data(applied).get("recordId"));
        Long instanceId = number(data(applied).get("approvalInstanceId"));
        ApprovalTask task = taskMapper.selectOne(new LambdaQueryWrapper<ApprovalTask>()
                .eq(ApprovalTask::getInstanceId, instanceId));
        exchange("/web/approval/tasks/" + task.getId() + "/approve", HttpMethod.POST, null, teacherToken);

        exchange("/web/visitor/records/" + recordId + "/cancel", HttpMethod.POST, null, visitorToken);

        assertThat(recordMapper.selectById(recordId).getStatus()).isEqualTo(5);
        VisitorPass pass = passMapper.selectOne(new LambdaQueryWrapper<VisitorPass>()
                .eq(VisitorPass::getRecordId, recordId));
        assertThat(pass.getStatus()).isEqualTo(3);
    }

    @Test
    void batchCreateDirectlyIssuesPasses() {
        String body = """
                {"items":[
                  {"name":"访客A","phone":"13900000001","expectedTime":"%s","leaveTime":"%s"},
                  {"name":"访客B","phone":"13900000002","expectedTime":"%s","leaveTime":"%s"}
                ]}
                """.formatted(FORMAT.format(LocalDateTime.now().plusDays(1)),
                FORMAT.format(LocalDateTime.now().plusDays(1).plusHours(2)),
                FORMAT.format(LocalDateTime.now().plusDays(2)),
                FORMAT.format(LocalDateTime.now().plusDays(2).plusHours(2)));

        Map<String, Object> result = exchange("/web/admin/visitor-records/batch",
                HttpMethod.POST, body, adminToken);

        assertThat(result.get("code")).isEqualTo(200);
        assertThat(number(data(result).get("created"))).isEqualTo(2);
        assertThat(recordMapper.selectCount(null)).isEqualTo(2L);
        assertThat(passMapper.selectCount(null)).isEqualTo(2L);
        assertThat(recordMapper.selectList(null)).allMatch(record -> record.getStatus() == 1);
    }

    @Test
    void adminOnlySeesOwnSchoolRecords() {
        apply(visitorToken, "13800003001", 1);

        Map<String, Object> own = data(exchange("/web/admin/visitor-records?page=1&size=10",
                HttpMethod.GET, null, adminToken));
        Map<String, Object> other = data(exchange("/web/admin/visitor-records?page=1&size=10",
                HttpMethod.GET, null, otherAdminToken));

        assertThat(number(own.get("total"))).isEqualTo(1);
        assertThat(number(other.get("total"))).isZero();
    }

    @Test
    void expireMarksOverdueRecords() {
        VisitorRecord record = new VisitorRecord();
        record.setSchoolId(1L);
        record.setVisitorUserId(visitorUserId);
        record.setVisitorName("过期访客");
        record.setVisitorType("NORMAL");
        record.setExpectedTime(LocalDateTime.now().minusDays(2));
        record.setLeaveTime(LocalDateTime.now().minusDays(1));
        record.setStatus(1);
        record.setCreatedAt(LocalDateTime.now());
        record.setUpdatedAt(LocalDateTime.now());
        recordMapper.insert(record);

        int expired = visitorService.expireOverdue();

        assertThat(expired).isEqualTo(1);
        assertThat(recordMapper.selectById(record.getId()).getStatus()).isEqualTo(6);
    }

    @Test
    void applyRejectsInvalidTargetAndTime() {
        Map<String, Object> missing = exchange("/web/visitor/records", HttpMethod.POST,
                "{\"targetId\":999999,\"expectedTime\":\"2099-01-01 10:00:00\",\"leaveTime\":\"2099-01-01 12:00:00\"}",
                visitorToken);
        assertThat(missing.get("code")).isEqualTo(400);

        Map<String, Object> inverted = exchange("/web/visitor/records", HttpMethod.POST,
                "{\"targetId\":" + teacherStaffId + ",\"expectedTime\":\"2099-01-01 12:00:00\",\"leaveTime\":\"2099-01-01 10:00:00\"}",
                visitorToken);
        assertThat(inverted.get("code")).isEqualTo(400);
        assertThat(inverted.get("message")).asString().contains("离校时间");
    }

    @Test
    void passIsNotAvailableBeforeApproval() {
        Map<String, Object> applied = apply(visitorToken, "13800003001", 1);
        Long recordId = number(data(applied).get("recordId"));

        Map<String, Object> pass = exchange("/web/visitor/records/" + recordId + "/pass",
                HttpMethod.GET, null, visitorToken);

        assertThat(pass.get("code")).isEqualTo(404);
    }

    @Test
    void cancelAfterRejectIsRejected() {
        Map<String, Object> applied = apply(visitorToken, "13800003001", 1);
        Long recordId = number(data(applied).get("recordId"));
        Long instanceId = number(data(applied).get("approvalInstanceId"));
        ApprovalTask task = taskMapper.selectOne(new LambdaQueryWrapper<ApprovalTask>()
                .eq(ApprovalTask::getInstanceId, instanceId));
        exchange("/web/approval/tasks/" + task.getId() + "/reject", HttpMethod.POST, null, teacherToken);

        Map<String, Object> cancel = exchange("/web/visitor/records/" + recordId + "/cancel",
                HttpMethod.POST, null, visitorToken);

        assertThat(cancel.get("code")).isEqualTo(400);
    }
    private Map<String, Object> apply(String token, String phone, int offsetDays) {
        String body = """
                {"targetId":%d,"visitorType":"NORMAL","visitPurpose":"业务洽谈",
                 "expectedTime":"%s","leaveTime":"%s"}
                """.formatted(teacherStaffId,
                FORMAT.format(LocalDateTime.now().plusDays(offsetDays)),
                FORMAT.format(LocalDateTime.now().plusDays(offsetDays).plusHours(2)));
        return exchange("/web/visitor/records", HttpMethod.POST, body, token);
    }

    private Long createUser(String phone, RoleEnum role, Long schoolId) {
        User user = new User();
        user.setPhone(phone);
        user.setName("访客" + phone.substring(phone.length() - 4));
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

    private Long createStaff(String name, String phone, PersonTypeEnum type) {
        Staff staff = new Staff();
        staff.setSchoolId(1L);
        staff.setName(name);
        staff.setPhone(phone);
        staff.setPersonType(type);
        staff.setStatus(1);
        staff.setSyncSource("LOCAL");
        staff.setCreatedAt(LocalDateTime.now());
        staff.setUpdatedAt(LocalDateTime.now());
        staffMapper.insert(staff);
        return staff.getId();
    }

    private void insertFlow() {
        ApprovalFlow flow = new ApprovalFlow();
        flow.setFlowCode("VISIT_DEFAULT");
        flow.setName("默认访客流程");
        flow.setVersion(1);
        flow.setStatus(1);
        flow.setDefinitionJson("{\"code\":\"VISIT_DEFAULT\",\"name\":\"默认访客流程\",\"timeoutHours\":24,"
                + "\"nodes\":[{\"seq\":1,\"name\":\"被访人审批\",\"assignee\":\"TARGET\"}]}");
        flow.setCreatedAt(LocalDateTime.now());
        flow.setUpdatedAt(LocalDateTime.now());
        flowMapper.insert(flow);
    }

    private void insertRule() {
        ApprovalRule rule = new ApprovalRule();
        rule.setTargetType("TEACHER");
        rule.setFlowCode("VISIT_DEFAULT");
        rule.setPriority(10);
        rule.setStatus(1);
        rule.setCreatedAt(LocalDateTime.now());
        rule.setUpdatedAt(LocalDateTime.now());
        ruleMapper.insert(rule);
    }

    private String token(Long userId, String phone, RoleEnum role, Long schoolId) {
        return jwtUtil.generate(userId, "访客" + phone.substring(phone.length() - 4), List.of(role), role, schoolId);
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

    private Long number(Object value) {
        return ((Number) value).longValue();
    }
}
