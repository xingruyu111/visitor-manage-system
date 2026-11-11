package com.visitor.workflow;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.visitor.common.enums.PersonTypeEnum;
import com.visitor.common.enums.RoleEnum;
import com.visitor.common.exception.BizException;
import com.visitor.domain.entity.ApprovalFlow;
import com.visitor.domain.entity.ApprovalInstance;
import com.visitor.domain.entity.ApprovalRule;
import com.visitor.domain.entity.ApprovalTask;
import com.visitor.domain.entity.SchoolClass;
import com.visitor.domain.entity.Staff;
import com.visitor.domain.entity.User;
import com.visitor.domain.entity.UserRole;
import com.visitor.security.UserContext;
import com.visitor.server.mapper.ApprovalFlowMapper;
import com.visitor.server.mapper.ApprovalInstanceMapper;
import com.visitor.server.mapper.ApprovalRuleMapper;
import com.visitor.server.mapper.ApprovalTaskMapper;
import com.visitor.server.mapper.SchoolClassMapper;
import com.visitor.server.mapper.StaffMapper;
import com.visitor.server.mapper.UserMapper;
import com.visitor.server.mapper.UserRoleMapper;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@SpringBootTest
@ActiveProfiles("test")
class ApprovalEngineTest {

    @Autowired
    private ApprovalEngine engine;

    @Autowired
    private ApprovalFlowService flowService;

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

    private Long teacherUserId;
    private Long studentUserId;
    private Long counselorUserId;
    private Long teacherStaffId;
    private Long studentStaffId;

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

        teacherUserId = createUser("13800002001", RoleEnum.TEACHER, 1L);
        studentUserId = createUser("13800002002", RoleEnum.STUDENT, 1L);
        counselorUserId = createUser("13800002003", RoleEnum.TEACHER, 1L);
        createUser("13800002004", RoleEnum.SECURITY_ADMIN, 1L);

        teacherStaffId = createStaff("张老师", "13800002001", PersonTypeEnum.TEACHER, null, null);
        Long counselorStaffId = createStaff("辅导员", "13800002003", PersonTypeEnum.TEACHER, null, null);
        SchoolClass schoolClass = createClass("计算机2201", counselorStaffId);
        studentStaffId = createStaff("学生甲", "13800002002", PersonTypeEnum.STUDENT,
                schoolClass.getId(), counselorStaffId);

        insertFlow("VISIT_DEFAULT", "{\"code\":\"VISIT_DEFAULT\",\"name\":\"默认\",\"timeoutHours\":24,"
                + "\"nodes\":[{\"seq\":1,\"name\":\"被访人审批\",\"assignee\":\"TARGET\"}]}");
        insertFlow("VISIT_STUDENT", "{\"code\":\"VISIT_STUDENT\",\"name\":\"学生\",\"timeoutHours\":24,"
                + "\"nodes\":[{\"seq\":1,\"name\":\"学生审批\",\"assignee\":\"TARGET\"},"
                + "{\"seq\":2,\"name\":\"辅导员审批\",\"assignee\":\"COUNSELOR\"}]}");
        insertRule("TEACHER", "VISIT_DEFAULT");
        insertRule("STUDENT", "VISIT_STUDENT");
    }

    @AfterEach
    void tearDown() {
        UserContext.clear();
    }

    @Test
    void startIsIdempotentForSameBusinessId() {
        ApprovalInstance first = start(1L, "TEACHER", teacherStaffId);
        ApprovalInstance second = start(1L, "TEACHER", teacherStaffId);

        assertThat(second.getId()).isEqualTo(first.getId());
        assertThat(taskMapper.selectCount(null)).isEqualTo(1L);
    }

    @Test
    void startFailsWhenFlowNotPublished() {
        flowMapper.delete(null);

        assertThatThrownBy(() -> start(2L, "TEACHER", teacherStaffId))
                .isInstanceOf(BizException.class)
                .hasMessageContaining("审批流程未发布");
    }

    @Test
    void cancelSkipsPendingTasks() {
        ApprovalInstance instance = start(3L, "TEACHER", teacherStaffId);
        asAdmin(1L);

        engine.cancel(instance.getId(), "申请人取消");

        assertThat(instanceMapper.selectById(instance.getId()).getStatus()).isEqualTo(ApprovalStatuses.CANCELLED);
        assertThat(taskMapper.selectList(new LambdaQueryWrapper<ApprovalTask>()
                .eq(ApprovalTask::getInstanceId, instance.getId())).get(0).getStatus())
                .isEqualTo(ApprovalStatuses.SKIPPED);
    }

    @Test
    void approveTwiceIsRejected() {
        ApprovalInstance instance = start(4L, "TEACHER", teacherStaffId);
        ApprovalTask task = firstTask(instance.getId());
        as(teacherUserId, RoleEnum.TEACHER, 1L);
        engine.approve(task.getId(), "ok", false);

        assertThatThrownBy(() -> engine.approve(task.getId(), "again", false))
                .isInstanceOf(BizException.class)
                .hasMessageContaining("任务已处理");
    }

    @Test
    void myTasksOnlyReturnsAssignedPendingTasks() {
        start(5L, "TEACHER", teacherStaffId);
        start(6L, "STUDENT", studentStaffId);

        as(teacherUserId, RoleEnum.TEACHER, 1L);
        assertThat(engine.myTasks(1, 10).total()).isEqualTo(1);

        as(studentUserId, RoleEnum.STUDENT, 1L);
        assertThat(engine.myTasks(1, 10).total()).isEqualTo(1);
    }

    @Test
    void instancesCanBeFilteredByStatus() {
        ApprovalInstance instance = start(7L, "TEACHER", teacherStaffId);
        ApprovalTask task = firstTask(instance.getId());
        as(teacherUserId, RoleEnum.TEACHER, 1L);
        engine.approve(task.getId(), "ok", false);

        assertThat(engine.instances(1L, ApprovalStatuses.APPROVED, 1, 10).total()).isEqualTo(1);
        assertThat(engine.instances(1L, ApprovalStatuses.RUNNING, 1, 10).total()).isZero();
    }

    @Test
    void rejectSkipsRemainingNodes() {
        ApprovalInstance instance = start(8L, "STUDENT", studentStaffId);
        ApprovalTask first = firstTask(instance.getId());
        as(studentUserId, RoleEnum.STUDENT, 1L);

        engine.reject(first.getId(), "材料不全", false);

        assertThat(instanceMapper.selectById(instance.getId()).getStatus()).isEqualTo(ApprovalStatuses.REJECTED);
        assertThat(taskMapper.selectCount(null)).isEqualTo(1L);
    }

    @Test
    void previewResolvesAssigneeNames() {
        List<ApprovalFlowService.PreviewNode> nodes = flowService.preview("VISIT_STUDENT", studentStaffId);

        assertThat(nodes).hasSize(2);
        assertThat(nodes.get(0).assigneeId()).isEqualTo(studentUserId);
        assertThat(nodes.get(0).assigneeName()).isEqualTo("用户2002");
        assertThat(nodes.get(1).assigneeId()).isEqualTo(counselorUserId);
    }

    @Test
    void flowCanBeOfflinedAndVersioned() {
        ApprovalFlow draft = flowService.saveDraft("VISIT_DEFAULT",
                new FlowDefinition("VISIT_DEFAULT", "默认 v2", 12, List.of(
                        new FlowDefinition.Node(1, "被访人审批", "TARGET", null))), 1L);
        assertThat(draft.getVersion()).isEqualTo(2);
        assertThat(draft.getStatus()).isZero();

        flowService.publish("VISIT_DEFAULT", 1L);
        assertThat(flowService.versions("VISIT_DEFAULT")).hasSize(2);

        flowService.offline("VISIT_DEFAULT", 1L);
        assertThatThrownBy(() -> flowService.offline("VISIT_DEFAULT", 1L))
                .isInstanceOf(BizException.class);
    }

    @Test
    void flowDetailAndValidationEdgeCases() {
        assertThat(flowService.detail("VISIT_DEFAULT", 1)).isNotNull();
        assertThatThrownBy(() -> flowService.detail("VISIT_DEFAULT", 99)).hasMessageContaining("流程版本不存在");
        assertThat(flowService.validate("UNKNOWN")).containsExactly("流程不存在");
        assertThatThrownBy(() -> flowService.preview("UNKNOWN", teacherStaffId))
                .isInstanceOf(BizException.class);
        assertThatThrownBy(() -> flowService.saveDraft("VISIT_DEFAULT",
                new FlowDefinition("OTHER", "x", 24, List.of()), 1L))
                .hasMessageContaining("流程编码与路径不一致");
    }

    @Test
    void draftUpdateKeepsSameVersion() {
        FlowDefinition definition = new FlowDefinition("VISIT_DEFAULT", "v2", 24, List.of(
                new FlowDefinition.Node(1, "被访人审批", "TARGET", null)));
        ApprovalFlow first = flowService.saveDraft("VISIT_DEFAULT", definition, 1L);
        ApprovalFlow second = flowService.saveDraft("VISIT_DEFAULT", definition, 1L);

        assertThat(second.getId()).isEqualTo(first.getId());
        assertThat(second.getVersion()).isEqualTo(first.getVersion());
    }
    @Test
    void securityReviewNodeIsHandledBySecurityAdmin() {
        insertFlow("VISIT_REVIEW", "{\"code\":\"VISIT_REVIEW\",\"name\":\"复核流程\",\"timeoutHours\":24,"
                + "\"nodes\":[{\"seq\":1,\"name\":\"被访人审批\",\"assignee\":\"TARGET\"},"
                + "{\"seq\":2,\"name\":\"保卫复核\",\"assignee\":\"SECURITY\"}]}");

        ApprovalInstance instance = engine.start(new ApprovalEngine.StartCommand(
                "VISITOR_RECORD", 99L, 1L, studentUserId, "NORMAL", "REVIEW", teacherStaffId));
        ApprovalTask first = firstTask(instance.getId());
        as(teacherUserId, RoleEnum.TEACHER, 1L);
        engine.approve(first.getId(), "同意", false);

        ApprovalTask review = taskMapper.selectOne(new LambdaQueryWrapper<ApprovalTask>()
                .eq(ApprovalTask::getInstanceId, instance.getId())
                .eq(ApprovalTask::getSeq, 2));
        Long securityUserId = userMapper.selectOne(new LambdaQueryWrapper<User>()
                .eq(User::getPhone, "13800002004")).getId();
        assertThat(review.getAssigneeId()).isEqualTo(securityUserId);
        assertThat(review.getStatus()).isEqualTo(ApprovalStatuses.PENDING);

        as(securityUserId, RoleEnum.SECURITY_ADMIN, 1L);
        engine.approve(review.getId(), "复核通过", false);

        assertThat(instanceMapper.selectById(instance.getId()).getStatus()).isEqualTo(ApprovalStatuses.APPROVED);
    }
    @Test
    void flowAssociationByTargetTypeTakesPrecedence() {
        ApprovalFlow flow = new ApprovalFlow();
        flow.setFlowCode("VISIT_BY_TARGET");
        flow.setName("按被访人类型流程");
        flow.setVersion(1);
        flow.setStatus(1);
        flow.setDefinitionJson("{\"code\":\"VISIT_BY_TARGET\",\"name\":\"按被访人类型流程\","
                + "\"targetTypes\":[\"TEACHER\"],"
                + "\"nodes\":[{\"seq\":1,\"name\":\"按类型审批\",\"assignee\":\"TARGET\"}]}");
        flow.setCreatedAt(LocalDateTime.now());
        flow.setUpdatedAt(LocalDateTime.now());
        flowMapper.insert(flow);

        ApprovalInstance instance = engine.start(new ApprovalEngine.StartCommand(
                "VISITOR_RECORD", 88L, 1L, studentUserId, "NORMAL", "TEACHER", teacherStaffId));

        assertThat(instance.getFlowCode()).isEqualTo("VISIT_BY_TARGET");
    }

    private ApprovalInstance start(Long bizId, String targetType, Long targetId) {
        return engine.start(new ApprovalEngine.StartCommand(
                "VISITOR_RECORD", bizId, 1L, studentUserId, "NORMAL", targetType, targetId));
    }

    private ApprovalTask firstTask(Long instanceId) {
        return taskMapper.selectOne(new LambdaQueryWrapper<ApprovalTask>()
                .eq(ApprovalTask::getInstanceId, instanceId)
                .eq(ApprovalTask::getSeq, 1));
    }

    private void as(Long userId, RoleEnum role, Long schoolId) {
        UserContext.set(userId, "user-" + userId, role, schoolId);
    }

    private void asAdmin(Long schoolId) {
        User user = userMapper.selectOne(new LambdaQueryWrapper<User>().eq(User::getPhone, "13800002004"));
        as(user.getId(), RoleEnum.SECURITY_ADMIN, schoolId);
    }

    private Long createUser(String phone, RoleEnum role, Long schoolId) {
        User user = new User();
        user.setPhone(phone);
        user.setName("用户" + phone.substring(phone.length() - 4));
        user.setPassword("x");
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

    private void insertFlow(String code, String json) {
        ApprovalFlow flow = new ApprovalFlow();
        flow.setFlowCode(code);
        flow.setName(code);
        flow.setVersion(1);
        flow.setStatus(1);
        flow.setDefinitionJson(json);
        flow.setCreatedAt(LocalDateTime.now());
        flow.setUpdatedAt(LocalDateTime.now());
        flowMapper.insert(flow);
    }

    private void insertRule(String targetType, String flowCode) {
        ApprovalRule rule = new ApprovalRule();
        rule.setTargetType(targetType);
        rule.setFlowCode(flowCode);
        rule.setPriority(10);
        rule.setStatus(1);
        rule.setCreatedAt(LocalDateTime.now());
        rule.setUpdatedAt(LocalDateTime.now());
        ruleMapper.insert(rule);
    }
}
