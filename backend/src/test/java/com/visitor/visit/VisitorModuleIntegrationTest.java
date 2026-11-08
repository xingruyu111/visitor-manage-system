package com.visitor.visit;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.visitor.common.enums.PersonTypeEnum;
import com.visitor.common.enums.RoleEnum;
import com.visitor.domain.entity.AccessList;
import com.visitor.domain.entity.ApprovalFlow;
import com.visitor.domain.entity.ApprovalRule;
import com.visitor.domain.entity.ApprovalTask;
import com.visitor.domain.entity.Staff;
import com.visitor.domain.entity.User;
import com.visitor.domain.entity.UserRole;
import com.visitor.domain.entity.VisitorPass;
import com.visitor.domain.entity.VisitorRecord;
import com.visitor.security.JwtUtil;
import com.visitor.server.mapper.AccessListMapper;
import com.visitor.server.mapper.ApprovalFlowMapper;
import com.visitor.server.mapper.ApprovalInstanceMapper;
import com.visitor.server.mapper.ApprovalRuleMapper;
import com.visitor.server.mapper.ApprovalTaskMapper;
import com.visitor.server.mapper.StaffMapper;
import com.visitor.server.mapper.UserMapper;
import com.visitor.server.mapper.UserRoleMapper;
import com.visitor.server.mapper.VisitLogMapper;
import com.visitor.server.mapper.FormFieldMapper;
import com.visitor.server.mapper.FormFieldMapper;
import com.visitor.server.mapper.IntegrationLogMapper;
import com.visitor.server.mapper.NotificationMapper;
import com.visitor.server.mapper.NotifyConfigMapper;
import com.visitor.server.mapper.ParentSlotMapper;
import com.visitor.server.mapper.VisitorPassMapper;
import com.visitor.server.mapper.VisitorRecordMapper;
import com.visitor.server.service.AccessCheckService;
import com.visitor.server.service.VisitorService;
import com.visitor.server.service.impl.VisitorPassIssuer;
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
    private AccessCheckService accessCheckService;

    @Autowired
    private AccessListMapper accessListMapper;

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
    private VisitLogMapper visitLogMapper;

    @Autowired
    private NotificationMapper notificationMapper;

    @Autowired
    private FormFieldMapper formFieldMapper;

    @Autowired
    private ParentSlotMapper parentSlotMapper;

    @Autowired
    private NotifyConfigMapper notifyConfigMapper;

    @Autowired
    private IntegrationLogMapper integrationLogMapper;

    @Autowired
    private VisitorPassIssuer passIssuer;

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
    private String deptToken;
    private String guardToken;
    private String otherAdminToken;

    @BeforeEach
    void setUp() {
        accessListMapper.delete(null);
        notificationMapper.delete(null);
        formFieldMapper.delete(null);
        parentSlotMapper.delete(null);
        notifyConfigMapper.delete(null);
        integrationLogMapper.delete(null);
        formFieldMapper.delete(null);
        parentSlotMapper.delete(null);
        notifyConfigMapper.delete(null);
        integrationLogMapper.delete(null);
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
        Long adminId = createUser("13800003004", RoleEnum.SECURITY_ADMIN, 1L);
        Long otherAdminId = createUser("13800003005", RoleEnum.SECURITY_ADMIN, 2L);

        teacherStaffId = createStaff("张老师", "13800003003", PersonTypeEnum.TEACHER);

        visitorToken = token(visitorUserId, "13800003001", RoleEnum.VISITOR, null);
        otherVisitorToken = token(otherVisitorId, "13800003002", RoleEnum.VISITOR, null);
        teacherToken = token(teacherUserId, "13800003003", RoleEnum.TEACHER, 1L);
        adminToken = token(adminId, "13800003004", RoleEnum.SECURITY_ADMIN, 1L);
        Long deptId = createUser("13800003006", RoleEnum.DEPT_OPERATOR, 1L);
        deptToken = token(deptId, "13800003006", RoleEnum.DEPT_OPERATOR, 1L);
        Long guardId = createUser("13800003007", RoleEnum.GUARD, 1L);
        guardToken = token(guardId, "13800003007", RoleEnum.GUARD, 1L);
        otherAdminToken = token(otherAdminId, "13800003005", RoleEnum.SECURITY_ADMIN, 2L);

        insertFlow();
        insertDeptFlow();
        insertRule();
    }

    @Test
    void applyCreatesPendingRecordAndTask() {
        Map<String, Object> result = apply(visitorToken, "13800003001", 0);

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
        Map<String, Object> applied = apply(visitorToken, "13800003001", 0);
        Long recordId = number(data(applied).get("recordId"));
        Long instanceId = number(data(applied).get("approvalInstanceId"));
        ApprovalTask task = taskMapper.selectOne(new LambdaQueryWrapper<ApprovalTask>()
                .eq(ApprovalTask::getInstanceId, instanceId));

        exchange("/web/approval/tasks/" + task.getId() + "/approve?comment=欢迎",
                HttpMethod.POST, null, teacherToken);

        assertThat(recordMapper.selectById(recordId).getStatus()).isEqualTo(1);
        VisitorPass pass = passMapper.selectOne(new LambdaQueryWrapper<VisitorPass>()
                .eq(VisitorPass::getRecordId, recordId));
        pass.setValidFrom(LocalDateTime.now().minusMinutes(1));
        passMapper.updateById(pass);
        assertThat(pass).isNotNull();
        assertThat(pass.getQrToken()).isNotBlank();
        assertThat(pass.getMaxEntries()).isEqualTo(1);

        Map<String, Object> passBody = data(exchange("/web/visitor/records/" + recordId + "/pass",
                HttpMethod.GET, null, visitorToken));
        assertThat(passBody.get("qrToken")).isEqualTo(pass.getQrToken());
    }

    @Test
    void rejectedApplicationHasNoPass() {
        Map<String, Object> applied = apply(visitorToken, "13800003001", 0);
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
        apply(visitorToken, "13800003001", 0);

        Map<String, Object> second = apply(visitorToken, "13800003001", 0);

        assertThat(second.get("code")).isEqualTo(400);
        assertThat(second.get("message")).asString().contains("已有预约");
    }

    @Test
    void visitorCannotReadOthersRecord() {
        Map<String, Object> applied = apply(visitorToken, "13800003001", 0);
        Long recordId = number(data(applied).get("recordId"));

        Map<String, Object> detail = exchange("/web/visitor/records/" + recordId,
                HttpMethod.GET, null, otherVisitorToken);

        assertThat(detail.get("code")).isEqualTo(404);
    }

    @Test
    void cancelRevokesPass() {
        Map<String, Object> applied = apply(visitorToken, "13800003001", 0);
        Long recordId = number(data(applied).get("recordId"));
        Long instanceId = number(data(applied).get("approvalInstanceId"));
        ApprovalTask task = taskMapper.selectOne(new LambdaQueryWrapper<ApprovalTask>()
                .eq(ApprovalTask::getInstanceId, instanceId));
        exchange("/web/approval/tasks/" + task.getId() + "/approve", HttpMethod.POST, null, teacherToken);

        exchange("/web/visitor/records/" + recordId + "/cancel", HttpMethod.POST, null, visitorToken);

        assertThat(recordMapper.selectById(recordId).getStatus()).isEqualTo(5);
        VisitorPass pass = passMapper.selectOne(new LambdaQueryWrapper<VisitorPass>()
                .eq(VisitorPass::getRecordId, recordId));
        pass.setValidFrom(LocalDateTime.now().minusMinutes(1));
        passMapper.updateById(pass);
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
    void singleSchoolShowsSameRecordsForAllAdmins() {
        apply(visitorToken, "13800003001", 0);

        Map<String, Object> own = data(exchange("/web/admin/visitor-records?page=1&size=10",
                HttpMethod.GET, null, adminToken));
        Map<String, Object> other = data(exchange("/web/admin/visitor-records?page=1&size=10",
                HttpMethod.GET, null, otherAdminToken));

        assertThat(number(own.get("total"))).isEqualTo(1);
        assertThat(number(other.get("total"))).isEqualTo(1);
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
        Map<String, Object> applied = apply(visitorToken, "13800003001", 0);
        Long recordId = number(data(applied).get("recordId"));

        Map<String, Object> pass = exchange("/web/visitor/records/" + recordId + "/pass",
                HttpMethod.GET, null, visitorToken);

        assertThat(pass.get("code")).isEqualTo(404);
    }

    @Test
    void cancelAfterRejectIsRejected() {
        Map<String, Object> applied = apply(visitorToken, "13800003001", 0);
        Long recordId = number(data(applied).get("recordId"));
        Long instanceId = number(data(applied).get("approvalInstanceId"));
        ApprovalTask task = taskMapper.selectOne(new LambdaQueryWrapper<ApprovalTask>()
                .eq(ApprovalTask::getInstanceId, instanceId));
        exchange("/web/approval/tasks/" + task.getId() + "/reject", HttpMethod.POST, null, teacherToken);

        Map<String, Object> cancel = exchange("/web/visitor/records/" + recordId + "/cancel",
                HttpMethod.POST, null, visitorToken);

        assertThat(cancel.get("code")).isEqualTo(400);
    }
    @Test
    void blacklistedVisitorIsBlocked() {
        insertList(1, "13800003001", null);

        Map<String, Object> result = apply(visitorToken, "13800003001", 0);

        assertThat(result.get("code")).isEqualTo(403);
        assertThat(result.get("message")).asString().contains("黑名单");
        assertThat(recordMapper.selectCount(null)).isZero();
    }

    @Test
    void whitelistedVisitorIsAutoApprovedWithPass() {
        insertList(2, "13800003001", null);

        Map<String, Object> result = apply(visitorToken, "13800003001", 0);

        assertThat(result.get("code")).isEqualTo(200);
        assertThat(data(result).get("approvalInstanceId")).isNull();
        Long recordId = number(data(result).get("recordId"));
        assertThat(recordMapper.selectById(recordId).getStatus()).isEqualTo(1);
        assertThat(passMapper.selectCount(null)).isEqualTo(1L);
    }

    @Test
    void expiredListEntryAutoExpires() {
        AccessList list = insertList(1, "13900009999", LocalDateTime.now().minusHours(1));

        int expired = accessCheckService.expireOverdue();

        assertThat(expired).isGreaterThanOrEqualTo(1);
        assertThat(accessListMapper.selectById(list.getId()).getStatus()).isZero();
    }

    @Test
    void verifyInOutAndEntryLimit() {
        Map<String, Object> applied = apply(visitorToken, "13800003001", 0);
        Long recordId = number(data(applied).get("recordId"));
        Long instanceId = number(data(applied).get("approvalInstanceId"));
        ApprovalTask task = taskMapper.selectOne(new LambdaQueryWrapper<ApprovalTask>()
                .eq(ApprovalTask::getInstanceId, instanceId));
        exchange("/web/approval/tasks/" + task.getId() + "/approve", HttpMethod.POST, null, teacherToken);
        VisitorPass pass = passMapper.selectOne(new LambdaQueryWrapper<VisitorPass>()
                .eq(VisitorPass::getRecordId, recordId));
        pass.setValidFrom(LocalDateTime.now().minusMinutes(1));
        passMapper.updateById(pass);

        Map<String, Object> in = exchange("/web/guard/verify?token=" + pass.getQrToken(),
                HttpMethod.POST, null, adminToken);
        assertThat(in.get("code")).isEqualTo(200);
        assertThat(data(in).get("direction")).isEqualTo("IN");
        assertThat(recordMapper.selectById(recordId).getStatus()).isEqualTo(3);

        Map<String, Object> out = exchange("/web/guard/verify?token=" + pass.getQrToken(),
                HttpMethod.POST, null, adminToken);
        assertThat(data(out).get("direction")).isEqualTo("OUT");
        assertThat(recordMapper.selectById(recordId).getStatus()).isEqualTo(4);

        Map<String, Object> again = exchange("/web/guard/verify?token=" + pass.getQrToken(),
                HttpMethod.POST, null, adminToken);
        assertThat(again.get("code")).isEqualTo(400);
        assertThat(again.get("message")).asString().contains("次数");

        assertThat(visitLogMapper.selectCount(null)).isEqualTo(2L);
    }

    @Test
    void verifyRejectsInvalidAndExpiredPass() {
        Map<String, Object> invalid = exchange("/web/guard/verify?token=not-exists",
                HttpMethod.POST, null, adminToken);
        assertThat(invalid.get("code")).isEqualTo(404);

        Map<String, Object> applied = apply(visitorToken, "13800003001", 0);
        Long recordId = number(data(applied).get("recordId"));
        Long instanceId = number(data(applied).get("approvalInstanceId"));
        ApprovalTask task = taskMapper.selectOne(new LambdaQueryWrapper<ApprovalTask>()
                .eq(ApprovalTask::getInstanceId, instanceId));
        exchange("/web/approval/tasks/" + task.getId() + "/approve", HttpMethod.POST, null, teacherToken);
        VisitorPass pass = passMapper.selectOne(new LambdaQueryWrapper<VisitorPass>()
                .eq(VisitorPass::getRecordId, recordId));
        pass.setValidFrom(LocalDateTime.now().minusMinutes(1));
        passMapper.updateById(pass);
        pass.setValidTo(LocalDateTime.now().minusHours(1));
        passMapper.updateById(pass);

        Map<String, Object> expired = exchange("/web/guard/verify?token=" + pass.getQrToken(),
                HttpMethod.POST, null, adminToken);
        assertThat(expired.get("code")).isEqualTo(400);
        assertThat(expired.get("message")).asString().contains("过期");
    }
    private AccessList insertList(int listType, String value, LocalDateTime effectiveTo) {
        AccessList list = new AccessList();
        list.setSchoolId(1L);
        list.setListType(listType);
        list.setTargetType("PERSON");
        list.setTargetValue(value);
        list.setEffectiveTo(effectiveTo);
        list.setStatus(1);
        list.setSource("MANUAL");
        list.setCreatedAt(LocalDateTime.now());
        list.setUpdatedAt(LocalDateTime.now());
        accessListMapper.insert(list);
        return list;
    }
    @Test
    void approvalResultCreatesNotification() {
        Map<String, Object> applied = apply(visitorToken, "13800003001", 0);
        Long instanceId = number(data(applied).get("approvalInstanceId"));
        ApprovalTask task = taskMapper.selectOne(new LambdaQueryWrapper<ApprovalTask>()
                .eq(ApprovalTask::getInstanceId, instanceId));
        exchange("/web/approval/tasks/" + task.getId() + "/approve", HttpMethod.POST, null, teacherToken);

        assertThat(notificationMapper.selectCount(new LambdaQueryWrapper<com.visitor.domain.entity.Notification>()
                .eq(com.visitor.domain.entity.Notification::getUserId, visitorUserId))).isEqualTo(1L);

        Map<String, Object> list = data(exchange("/web/notifications?page=1&size=10",
                HttpMethod.GET, null, visitorToken));
        assertThat(number(list.get("total"))).isEqualTo(1);

        Map<String, Object> unread = data(exchange("/web/notifications/unread-count",
                HttpMethod.GET, null, visitorToken));
        assertThat(number(unread.get("count"))).isEqualTo(1);

        exchange("/web/notifications/read-all", HttpMethod.POST, null, visitorToken);
        Map<String, Object> after = data(exchange("/web/notifications/unread-count",
                HttpMethod.GET, null, visitorToken));
        assertThat(number(after.get("count"))).isZero();
    }
    @Test
    void applyNotifiesVisitedPerson() {
        apply(visitorToken, "13800003001", 0);

        assertThat(notificationMapper.selectCount(new LambdaQueryWrapper<com.visitor.domain.entity.Notification>()
                .eq(com.visitor.domain.entity.Notification::getUserId, teacherUserId))).isEqualTo(1L);
    }
    @Test
    void requiredFormFieldBlocksApplication() {
        com.visitor.domain.entity.FormField field = new com.visitor.domain.entity.FormField();
        field.setVisitorType("NORMAL");
        field.setFieldKey("company");
        field.setLabel("工作单位");
        field.setFieldType("TEXT");
        field.setRequired(1);
        field.setStatus(1);
        formFieldMapper.insert(field);

        Map<String, Object> result = apply(visitorToken, "13800003001", 0);

        assertThat(result.get("code")).isEqualTo(400);
        assertThat(result.get("message")).asString().contains("工作单位");
        assertThat(recordMapper.selectCount(null)).isZero();
    }
    @Test
    void deptBatchCreatesPendingRecordsAndSecurityApproves() {
        String body = """
                {"items":[{"name":"部门访客","phone":"13900008888","expectedTime":"%s","leaveTime":"%s"}]}
                """.formatted(FORMAT.format(LocalDateTime.now()), FORMAT.format(LocalDateTime.now().plusHours(2)));

        Map<String, Object> result = exchange("/web/dept/visitor-records/batch",
                HttpMethod.POST, body, deptToken);
        assertThat(result.get("code")).isEqualTo(200);
        assertThat(number(data(result).get("created"))).isEqualTo(1);

        VisitorRecord record = recordMapper.selectOne(null);
        assertThat(record.getStatus()).isZero();
        ApprovalTask task = taskMapper.selectOne(new LambdaQueryWrapper<ApprovalTask>()
                .eq(ApprovalTask::getInstanceId, taskMapper.selectList(null).get(0).getInstanceId()));
        assertThat(task.getAssigneeId()).isNull();

        exchange("/web/approval/tasks/" + task.getId() + "/approve", HttpMethod.POST, null, adminToken);

        assertThat(recordMapper.selectById(record.getId()).getStatus()).isEqualTo(1);
        assertThat(passMapper.selectCount(null)).isEqualTo(1L);
    }

    @Test
    void alumniPassIsMonthly() {
        VisitorRecord record = new VisitorRecord();
        record.setSchoolId(1L);
        record.setVisitorUserId(visitorUserId);
        record.setVisitorName("校友");
        record.setVisitorType("ALUMNI");
        record.setExpectedTime(LocalDateTime.now());
        record.setLeaveTime(LocalDateTime.now().plusHours(2));
        record.setStatus(1);
        record.setCreatedAt(LocalDateTime.now());
        record.setUpdatedAt(LocalDateTime.now());
        recordMapper.insert(record);

        VisitorPass pass = passIssuer.issue(record);

        assertThat(pass.getMaxEntries()).isEqualTo(30);
        assertThat(pass.getValidTo()).isAfter(record.getExpectedTime().plusDays(29));
    }

    private void insertDeptFlow() {
        ApprovalFlow flow = new ApprovalFlow();
        flow.setFlowCode("VISIT_DEPT_BATCH");
        flow.setName("部门批量预约审批");
        flow.setVersion(1);
        flow.setStatus(1);
        flow.setDefinitionJson("{\"code\":\"VISIT_DEPT_BATCH\",\"name\":\"部门批量预约审批\",\"timeoutHours\":24,"
                + "\"nodes\":[{\"seq\":1,\"name\":\"保卫处审批\",\"assignee\":\"SECURITY\"}]}");
        flow.setCreatedAt(LocalDateTime.now());
        flow.setUpdatedAt(LocalDateTime.now());
        flowMapper.insert(flow);
    }
    @Test
    void parentMustApplyWithinConfiguredSlot() {
        java.time.LocalDate monday = java.time.LocalDate.now()
                .with(java.time.temporal.TemporalAdjusters.next(java.time.DayOfWeek.MONDAY));
        com.visitor.domain.entity.ParentSlot slot = new com.visitor.domain.entity.ParentSlot();
        slot.setVisitorPhone("13800003001");
        slot.setWeekday(1);
        slot.setStartTime(java.time.LocalTime.of(8, 0));
        slot.setEndTime(java.time.LocalTime.of(18, 0));
        slot.setEffectiveFrom(monday.minusDays(7));
        slot.setEffectiveTo(monday.plusDays(7));
        slot.setStatus(1);
        parentSlotMapper.insert(slot);

        String okBody = """
                {"targetId":%d,"visitorType":"PARENT","expectedTime":"%s","leaveTime":"%s"}
                """.formatted(teacherStaffId, monday.atTime(9, 0).format(FORMAT), monday.atTime(10, 0).format(FORMAT));
        assertThat(exchange("/web/visitor/records", HttpMethod.POST, okBody, visitorToken).get("code")).isEqualTo(200);

        java.time.LocalDateTime tuesday = monday.plusDays(1).atTime(9, 0);
        String badBody = """
                {"targetId":%d,"visitorType":"PARENT","expectedTime":"%s","leaveTime":"%s"}
                """.formatted(teacherStaffId, tuesday.format(FORMAT), tuesday.plusHours(1).format(FORMAT));
        Map<String, Object> bad = exchange("/web/visitor/records", HttpMethod.POST, badBody, visitorToken);
        assertThat(bad.get("code")).isEqualTo(400);
        assertThat(bad.get("message")).asString().contains("家长固定时段");
    }
    @Test
    void guardCanQueryByPlateButVisitorCannot() {
        String body = """
                {"targetId":%d,"visitorType":"NORMAL","plateNumber":"粤b12345",
                 "expectedTime":"%s","leaveTime":"%s"}
                """.formatted(teacherStaffId, FORMAT.format(LocalDateTime.now()),
                FORMAT.format(LocalDateTime.now().plusHours(2)));
        Map<String, Object> applied = exchange("/web/visitor/records", HttpMethod.POST, body, visitorToken);
        Long instanceId = number(data(applied).get("approvalInstanceId"));
        ApprovalTask task = taskMapper.selectOne(new LambdaQueryWrapper<ApprovalTask>()
                .eq(ApprovalTask::getInstanceId, instanceId));
        exchange("/web/approval/tasks/" + task.getId() + "/approve", HttpMethod.POST, null, teacherToken);

        List<?> found = (List<?>) exchange("/web/guard/query?plate=粤B12345",
                HttpMethod.GET, null, guardToken).get("data");
        assertThat(found).hasSize(1);

        Map<String, Object> forbidden = exchange("/web/guard/query?plate=粤B12345",
                HttpMethod.GET, null, visitorToken);
        assertThat(forbidden.get("code")).isEqualTo(403);
    }

    @Test
    void expiredApprovedRecordExpiresPass() {
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
        VisitorPass pass = passIssuer.issue(record);

        visitorService.expireOverdue();

        assertThat(recordMapper.selectById(record.getId()).getStatus()).isEqualTo(6);
        assertThat(passMapper.selectById(pass.getId()).getStatus()).isEqualTo(2);
    }
    @Test
    void disabledNotifyTypeSuppressesNotifications() {
        com.visitor.domain.entity.NotifyConfig config = new com.visitor.domain.entity.NotifyConfig();
        config.setNotifyType("APPROVAL_RESULT");
        config.setEnabled(0);
        config.setChannels("SITE");
        notifyConfigMapper.insert(config);

        Map<String, Object> applied = apply(visitorToken, "13800003001", 0);
        Long instanceId = number(data(applied).get("approvalInstanceId"));
        ApprovalTask task = taskMapper.selectOne(new LambdaQueryWrapper<ApprovalTask>()
                .eq(ApprovalTask::getInstanceId, instanceId));
        exchange("/web/approval/tasks/" + task.getId() + "/approve", HttpMethod.POST, null, teacherToken);

        assertThat(notificationMapper.selectCount(null)).isZero();
    }
    @Test
    void visitorCanSearchTargets() {
        List<?> targets = (List<?>) exchange("/web/visitor/targets?keyword=张",
                HttpMethod.GET, null, visitorToken).get("data");

        assertThat(targets).hasSize(1);
    }
    @Test
    void visitorReadsActiveFormFields() {
        com.visitor.domain.entity.FormField field = new com.visitor.domain.entity.FormField();
        field.setVisitorType("ALUMNI");
        field.setFieldKey("company");
        field.setLabel("工作单位");
        field.setFieldType("TEXT");
        field.setRequired(1);
        field.setStatus(1);
        formFieldMapper.insert(field);

        List<?> fields = (List<?>) exchange("/web/visitor/form-fields?visitorType=ALUMNI",
                HttpMethod.GET, null, visitorToken).get("data");

        assertThat(fields).hasSize(1);
    }
    @Test
    void exportReturnsExcelFile() {
        apply(visitorToken, "13800003001", 0);

        org.springframework.http.ResponseEntity<byte[]> response = rest.exchange(
                "http://localhost:" + port + "/api/web/admin/visitor-records/export",
                HttpMethod.GET,
                new org.springframework.http.HttpEntity<>(new org.springframework.http.HttpHeaders() {{
                    setBearerAuth(adminToken);
                }}),
                byte[].class);

        assertThat(response.getStatusCode().value()).isEqualTo(200);
        assertThat(response.getHeaders().getContentType().toString()).contains("spreadsheetml");
        assertThat(response.getBody()).hasSizeGreaterThan(100);
    }
    @Test
    void auditLogRecordsApplyAndSupportsQueryExport() {
        apply(visitorToken, "13800003001", 0);

        Map<String, Object> page = data(exchange("/web/admin/audit-logs?action=VISITOR_APPLY",
                HttpMethod.GET, null, adminToken));
        assertThat(number(page.get("total"))).isGreaterThanOrEqualTo(1);

        org.springframework.http.ResponseEntity<byte[]> response = rest.exchange(
                "http://localhost:" + port + "/api/web/admin/audit-logs/export?action=VISITOR_APPLY",
                HttpMethod.GET,
                new org.springframework.http.HttpEntity<>(new org.springframework.http.HttpHeaders() {{
                    setBearerAuth(adminToken);
                }}),
                byte[].class);
        assertThat(new String(response.getBody(), java.nio.charset.StandardCharsets.UTF_8))
                .contains("VISITOR_APPLY");
    }
    @Test
    void integrationLogExportReturnsCsv() {
        org.springframework.http.ResponseEntity<byte[]> response = rest.exchange(
                "http://localhost:" + port + "/api/web/admin/integration-logs/export?success=0",
                HttpMethod.GET,
                new org.springframework.http.HttpEntity<>(new org.springframework.http.HttpHeaders() {{
                    setBearerAuth(adminToken);
                }}),
                byte[].class);

        assertThat(response.getStatusCode().value()).isEqualTo(200);
        assertThat(new String(response.getBody(), java.nio.charset.StandardCharsets.UTF_8))
                .contains("接口");
    }
    @Test
    void visitLogExportReturnsCsv() {
        org.springframework.http.ResponseEntity<byte[]> response = rest.exchange(
                "http://localhost:" + port + "/api/web/admin/visit-logs/export",
                HttpMethod.GET,
                new org.springframework.http.HttpEntity<>(new org.springframework.http.HttpHeaders() {{
                    setBearerAuth(adminToken);
                }}),
                byte[].class);

        assertThat(response.getStatusCode().value()).isEqualTo(200);
        assertThat(new String(response.getBody(), java.nio.charset.StandardCharsets.UTF_8)).contains("方向");
    }
    @Test
    void visitLogPageRequiresAdmin() {
        Map<String, Object> page = data(exchange("/web/admin/visit-logs?page=1&size=5",
                HttpMethod.GET, null, adminToken));
        assertThat(page.get("total")).isNotNull();

        Map<String, Object> forbidden = exchange("/web/admin/visit-logs", HttpMethod.GET, null, visitorToken);
        assertThat(forbidden.get("code")).isEqualTo(403);
    }
    @Test
    void accessListHttpCrud() {
        Map<String, Object> created = data(exchange("/web/admin/access-list", HttpMethod.POST,
                Map.of("listType", 1, "targetType", "PERSON", "targetValue", "13800007777"), adminToken));
        Long id = number(created.get("id"));

        Map<String, Object> page = data(exchange("/web/admin/access-list?listType=1",
                HttpMethod.GET, null, adminToken));
        assertThat(number(page.get("total"))).isGreaterThanOrEqualTo(1);

        Map<String, Object> released = exchange("/web/admin/access-list/" + id + "/release?reason=test",
                HttpMethod.POST, null, adminToken);
        assertThat(released.get("code")).isEqualTo(200);
    }

    @Test
    void formFieldAndParentSlotHttpCrud() {
        Map<String, Object> field = data(exchange("/web/admin/form-fields", HttpMethod.POST,
                Map.of("visitorType", "NORMAL", "fieldKey", "k1", "label", "字段1"), adminToken));
        assertThat(field.get("fieldKey")).isEqualTo("k1");
        assertThat((List<?>) exchange("/web/admin/form-fields?visitorType=NORMAL",
                HttpMethod.GET, null, adminToken).get("data")).hasSize(1);

        Map<String, Object> slotResp = exchange("/web/admin/parent-slots", HttpMethod.POST,
                Map.of("visitorPhone", "13800007778", "weekday", 1,
                        "startTime", "08:00:00", "endTime", "10:00:00"), adminToken);
        assertThat(slotResp.get("code")).as(String.valueOf(slotResp.get("message"))).isEqualTo(200);
        Map<String, Object> slot = data(slotResp);
        assertThat(slot.get("visitorPhone")).isEqualTo("13800007778");
        assertThat((List<?>) exchange("/web/admin/parent-slots?visitorPhone=13800007778",
                HttpMethod.GET, null, adminToken).get("data")).hasSize(1);
    }

    @Test
    void visitLogAndIntegrationLogExportWithData() {
        Map<String, Object> applied = apply(visitorToken, "13800003001", 0);
        Long recordId = number(data(applied).get("recordId"));
        com.visitor.domain.entity.VisitLog log = new com.visitor.domain.entity.VisitLog();
        log.setSchoolId(1L);
        log.setRecordId(recordId);
        log.setDirection("IN");
        log.setSource("LOCAL");
        log.setEventTime(LocalDateTime.now());
        visitLogMapper.insert(log);

        com.visitor.domain.entity.IntegrationLog ilog = new com.visitor.domain.entity.IntegrationLog();
        ilog.setIntegrationKey("GATE");
        ilog.setAction("pushPermission");
        ilog.setSuccess(0);
        ilog.setErrorCode("TIMEOUT");
        ilog.setCreatedAt(LocalDateTime.now());
        integrationLogMapper.insert(ilog);

        org.springframework.http.ResponseEntity<byte[]> visitCsv = rest.exchange(
                "http://localhost:" + port + "/api/web/admin/visit-logs/export",
                HttpMethod.GET,
                new org.springframework.http.HttpEntity<>(new org.springframework.http.HttpHeaders() {{
                    setBearerAuth(adminToken);
                }}),
                byte[].class);
        assertThat(new String(visitCsv.getBody(), java.nio.charset.StandardCharsets.UTF_8)).contains("访客");

        org.springframework.http.ResponseEntity<byte[]> logCsv = rest.exchange(
                "http://localhost:" + port + "/api/web/admin/integration-logs/export?success=0",
                HttpMethod.GET,
                new org.springframework.http.HttpEntity<>(new org.springframework.http.HttpHeaders() {{
                    setBearerAuth(adminToken);
                }}),
                byte[].class);
        assertThat(new String(logCsv.getBody(), java.nio.charset.StandardCharsets.UTF_8)).contains("GATE");
    }
    @Test
    void passModeIsValidatedAndStored() {
        String fmt = "yyyy-MM-dd HH:mm:ss";
        String badCar = """
                {"targetId":%d,"visitorType":"NORMAL","passMode":"CAR","expectedTime":"%s","leaveTime":"%s"}
                """.formatted(teacherStaffId, LocalDateTime.now().plusDays(1).format(java.time.format.DateTimeFormatter.ofPattern(fmt)),
                LocalDateTime.now().plusDays(1).plusHours(2).format(java.time.format.DateTimeFormatter.ofPattern(fmt)));
        Map<String, Object> carRejected = exchange("/web/visitor/records", HttpMethod.POST, badCar, visitorToken);
        assertThat(carRejected.get("code")).isEqualTo(400);
        assertThat(carRejected.get("message")).asString().contains("车牌号");

        String ebike = """
                {"targetId":%d,"visitorType":"NORMAL","passMode":"EBIKE","plateNumber":"粤B12345","expectedTime":"%s","leaveTime":"%s"}
                """.formatted(teacherStaffId, LocalDateTime.now().plusDays(2).format(java.time.format.DateTimeFormatter.ofPattern(fmt)),
                LocalDateTime.now().plusDays(2).plusHours(2).format(java.time.format.DateTimeFormatter.ofPattern(fmt)));
        Map<String, Object> ok = data(exchange("/web/visitor/records", HttpMethod.POST, ebike, visitorToken));
        assertThat(recordMapper.selectById(number(ok.get("recordId"))).getPassMode()).isEqualTo("EBIKE");

        Map<String, Object> walk = apply(visitorToken, "13800003001", 0);
        assertThat(recordMapper.selectById(number(data(walk).get("recordId"))).getPassMode()).isEqualTo("WALK");
    }
    @Test
    void preCheckReportsBlacklistAndWhitelist() {
        Map<String, Object> none = data(exchange("/web/visitor/records/pre-check",
                HttpMethod.GET, null, visitorToken));
        assertThat(none.get("status")).isEqualTo("NONE");

        insertList(1, "13800003001", null);
        Map<String, Object> black = data(exchange("/web/visitor/records/pre-check?plate=粤B00001",
                HttpMethod.GET, null, visitorToken));
        assertThat(black.get("status")).isEqualTo("BLACKLIST");
        assertThat(black.get("message")).asString().contains("黑名单");
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
