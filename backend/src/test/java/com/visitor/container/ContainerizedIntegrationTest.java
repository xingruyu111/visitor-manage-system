package com.visitor.container;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.visitor.domain.entity.AccessList;
import com.visitor.domain.entity.AlarmRecord;
import com.visitor.domain.entity.VisitorRecord;
import com.visitor.domain.entity.FormField;
import com.visitor.domain.entity.ParentSlot;
import com.visitor.domain.entity.NotifyConfig;
import com.visitor.domain.entity.ApprovalFlow;
import com.visitor.domain.entity.VisitLog;
import com.visitor.domain.entity.ApprovalRule;
import com.visitor.domain.entity.ApprovalTask;
import com.visitor.domain.entity.VisitorPass;
import com.visitor.domain.entity.VisitorRecord;
import com.visitor.server.mapper.AccessListMapper;
import com.visitor.server.mapper.AlarmRecordMapper;
import com.visitor.server.mapper.IntegrationConfigMapper;
import com.visitor.server.mapper.FormFieldMapper;
import com.visitor.server.mapper.ParentSlotMapper;
import com.visitor.server.mapper.NotifyConfigMapper;
import com.visitor.server.mapper.NotificationMapper;
import com.visitor.server.mapper.ApprovalFlowMapper;
import com.visitor.server.mapper.VisitLogMapper;
import com.visitor.server.mapper.ApprovalRuleMapper;
import com.visitor.server.mapper.ApprovalTaskMapper;
import com.visitor.server.mapper.VisitorPassMapper;
import com.visitor.server.mapper.VisitorRecordMapper;
import com.visitor.integration.gate.GateClient;
import com.visitor.integration.gate.MockGateClient;
import com.visitor.security.JwtUtil;
import com.visitor.server.service.AlarmService;
import com.visitor.server.service.GateRecordIntakeService;
import java.time.format.DateTimeFormatter;
import com.github.dockerjava.api.model.HostConfig;
import com.visitor.common.enums.PersonTypeEnum;
import com.visitor.common.enums.RoleEnum;
import com.visitor.domain.entity.Department;
import com.visitor.domain.entity.IntegrationConfig;
import com.visitor.domain.entity.SchoolClass;
import com.visitor.domain.entity.Staff;
import com.visitor.domain.entity.User;
import com.visitor.domain.entity.UserRole;
import com.visitor.server.mapper.DepartmentMapper;
import com.visitor.server.mapper.IntegrationConfigMapper;
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
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.MySQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.DockerImageName;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * 真实 MySQL + Redis 的容器化集成测试，覆盖 M2 鉴权与 M3 人员组织。
 *
 * <p>没有 Docker 的环境（如本机）自动跳过；CI 或装有 Docker 的服务器上会执行。</p>
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@Testcontainers(disabledWithoutDocker = true)
class ContainerizedIntegrationTest {

    @Container
    static final MySQLContainer<?> MYSQL = new MySQLContainer<>(DockerImageName.parse("mysql:8.0"))
            .withDatabaseName("visitor_test")
            .withUsername("visitor")
            .withPassword("visitor")
            .withCommand("--innodb-buffer-pool-size=16M", "--performance-schema=OFF",
                    "--max-connections=20", "--skip-name-resolve")
            .withCreateContainerCmdModifier(cmd -> {
                HostConfig hostConfig = cmd.getHostConfig() == null ? new HostConfig() : cmd.getHostConfig();
                cmd.withHostConfig(hostConfig.withMemory(384L * 1024 * 1024).withMemorySwap(384L * 1024 * 1024));
            });

    @Container
    static final GenericContainer<?> REDIS =
            new GenericContainer<>(DockerImageName.parse("redis:7-alpine"))
                    .withExposedPorts(6379)
                    .withCommand("redis-server", "--save", "", "--appendonly", "no", "--maxmemory", "32mb")
                    .withCreateContainerCmdModifier(cmd -> {
                        HostConfig hostConfig = cmd.getHostConfig() == null ? new HostConfig() : cmd.getHostConfig();
                        cmd.withHostConfig(hostConfig.withMemory(96L * 1024 * 1024).withMemorySwap(96L * 1024 * 1024));
                    });

    @DynamicPropertySource
    static void registerProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", MYSQL::getJdbcUrl);
        registry.add("spring.datasource.username", MYSQL::getUsername);
        registry.add("spring.datasource.password", MYSQL::getPassword);
        registry.add("spring.data.redis.host", REDIS::getHost);
        registry.add("spring.data.redis.port", () -> REDIS.getMappedPort(6379));
        registry.add("spring.sql.init.mode", () -> "always");
        registry.add("spring.sql.init.schema-locations", () -> "classpath:sql/schema-mysql.sql");
    }

    @LocalServerPort
    private int port;

    @Autowired
    private TestRestTemplate rest;

    @Autowired
    private IntegrationConfigMapper integrationConfigMapper;

    @Autowired
    private DepartmentMapper departmentMapper;

    @Autowired
    private StaffMapper staffMapper;

    @Autowired
    private SchoolClassMapper classMapper;

    @Autowired
    private UserMapper userMapper;

    @Autowired
    private UserRoleMapper userRoleMapper;

    @Autowired
    private ApprovalFlowMapper approvalFlowMapper;

    @Autowired
    private ApprovalRuleMapper approvalRuleMapper;

    @Autowired
    private ApprovalTaskMapper approvalTaskMapper;

    @Autowired
    private VisitorRecordMapper visitorRecordMapper;

    @Autowired
    private VisitorPassMapper visitorPassMapper;

    @Autowired
    private JwtUtil jwtUtil;

    @Autowired
    private AccessListMapper accessListMapper;

    @Autowired
    private VisitLogMapper visitLogMapper;

    @Autowired
    private FormFieldMapper formFieldMapper;

    @Autowired
    private ParentSlotMapper parentSlotMapper;

    @Autowired
    private NotifyConfigMapper notifyConfigMapper;

    @Autowired
    private NotificationMapper notificationMapper;

    @Autowired
    private AlarmRecordMapper alarmRecordMapper;

    @Autowired
    private AlarmService alarmService;

    @Autowired
    private GateRecordIntakeService gateRecordIntakeService;

    @Autowired
    private MockGateClient mockGateClient;



    @Autowired
    private PasswordEncoder passwordEncoder;

    @BeforeEach
    void setUp() {
        staffMapper.delete(null);
        accessListMapper.delete(null);
        visitLogMapper.delete(null);
        formFieldMapper.delete(null);
        parentSlotMapper.delete(null);
        notifyConfigMapper.delete(null);
        alarmRecordMapper.delete(null);
        mockGateClient.clear();
        classMapper.delete(null);
        departmentMapper.delete(null);
        integrationConfigMapper.delete(null);
        visitorPassMapper.delete(null);
        visitorRecordMapper.delete(null);
        enableDataSyncMock();
    }

    // ---------------- M2 鉴权 ----------------

    @Test
    void loginWorksAgainstRealMysql() {
        Map<String, Object> body = login("13800000000", "admin123");

        assertThat(body.get("code")).isEqualTo(200);
        assertThat(data(body).get("token")).asString().isNotBlank();
    }

    @Test
    void logoutInvalidatesTokenAgainstRealRedis() {
        String token = token(login("13800000000", "admin123"));

        rest.exchange(url("/auth/logout"), HttpMethod.POST,
                new HttpEntity<>(bearer(token)), String.class);
        ResponseEntity<String> replay = rest.exchange(url("/auth/me"), HttpMethod.GET,
                new HttpEntity<>(bearer(token)), String.class);

        assertThat(replay.getStatusCode().value()).isEqualTo(401);
    }

    @Test
    void fiveFailuresLockThePhoneAgainstRealRedis() {
        for (int i = 0; i < 5; i++) {
            assertThat(login("13900009999", "wrong").get("code")).isEqualTo(401);
        }

        Map<String, Object> sixth = login("13900009999", "wrong");

        assertThat(sixth.get("code")).isEqualTo(429);
        assertThat(sixth.get("message")).asString().contains("尝试过于频繁");
    }

    @Test
    void lockIsPerPhoneAndDoesNotAffectOthers() {
        for (int i = 0; i < 5; i++) {
            login("13900008888", "wrong");
        }

        assertThat(login("13800000000", "admin123").get("code")).isEqualTo(200);
    }

    // ---------------- M3 人员与组织 ----------------

    @Test
    void orgCrudWorksAgainstRealMysql() {
        String token = adminToken();

        Map<String, Object> dept = data(post("/web/admin/departments", token, Map.of("name", "信息工程学院")));
        Long deptId = ((Number) dept.get("id")).longValue();

        Map<String, Object> staff = data(post("/web/admin/staff", token,
                Map.of("name", "张老师", "personNo", "T02001", "phone", "13900002001", "personType", 1, "deptId", deptId)));

        assertThat(staff.get("deptName")).isEqualTo("信息工程学院");
        Map<String, Object> page = data(get("/web/admin/staff?page=1&size=10", token));
        assertThat(((Number) page.get("total")).intValue()).isEqualTo(1);
    }

    @Test
    void syncWorksAgainstRealMysql() {
        String token = adminToken();

        assertThat(((Number) data(post("/web/admin/org-sync/org", token, null)).get("created")).intValue())
                .isEqualTo(2);
        assertThat(((Number) data(post("/web/admin/org-sync/staff", token, null)).get("created")).intValue())
                .isEqualTo(3);

        Map<String, Object> second = data(post("/web/admin/org-sync/staff", token, null));
        assertThat(((Number) second.get("created")).intValue()).isZero();
        assertThat(((Number) second.get("updated")).intValue()).isEqualTo(3);
        assertThat(staffMapper.selectCount(null)).isEqualTo(3L);
    }

    @Test
    void syncDeactivatesMissingStaffAndRevokesRoleAgainstRealMysql() {
        Long userId = createUserWithRole("13900002099", RoleEnum.TEACHER);

        Staff legacy = new Staff();
        legacy.setSchoolId(1L);
        legacy.setName("离职老师");
        legacy.setPhone("13900002099");
        legacy.setPersonType(PersonTypeEnum.TEACHER);
        legacy.setStatus(1);
        legacy.setSyncSource("SYNC");
        legacy.setExternalId("T999");
        legacy.setCreatedAt(LocalDateTime.now());
        legacy.setUpdatedAt(LocalDateTime.now());
        staffMapper.insert(legacy);

        Map<String, Object> result = data(post("/web/admin/org-sync/staff", adminToken(), null));

        assertThat(((Number) result.get("deactivated")).intValue()).isEqualTo(1);
        assertThat(staffMapper.selectById(legacy.getId()).getStatus()).isZero();
        assertThat(userRoleMapper.selectList(new LambdaQueryWrapper<UserRole>()
                .eq(UserRole::getUserId, userId)).get(0).getStatus()).isZero();
    }

    @Test
    void visitorFlowFromApplyToPassWorksAgainstRealMysql() {
        Long teacherUserId = createUserWithRole("13900005001", RoleEnum.TEACHER);
        Staff teacher = new Staff();
        teacher.setSchoolId(1L);
        teacher.setName("张老师");
        teacher.setPhone("13900005001");
        teacher.setPersonType(PersonTypeEnum.TEACHER);
        teacher.setStatus(1);
        teacher.setSyncSource("LOCAL");
        teacher.setCreatedAt(LocalDateTime.now());
        teacher.setUpdatedAt(LocalDateTime.now());
        staffMapper.insert(teacher);
        insertApprovalFlowAndRule();

        Long visitorUserId = createUserWithRole("13900005002", RoleEnum.VISITOR);
        String visitorToken = jwtUtil.generate(visitorUserId, "访客", List.of(RoleEnum.VISITOR), RoleEnum.VISITOR, null);
        String teacherToken = jwtUtil.generate(teacherUserId, "张老师", List.of(RoleEnum.TEACHER), RoleEnum.TEACHER, 1L);

        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
        Map<String, Object> applied = data(post("/web/visitor/records", visitorToken, Map.of(
                "targetId", teacher.getId(),
                "visitorType", "NORMAL",
                "visitPurpose", "业务洽谈",
                "expectedTime", formatter.format(LocalDateTime.now().plusDays(1)),
                "leaveTime", formatter.format(LocalDateTime.now().plusDays(1).plusHours(2)))));

        Long recordId = ((Number) applied.get("recordId")).longValue();
        Long instanceId = ((Number) applied.get("approvalInstanceId")).longValue();
        ApprovalTask task = approvalTaskMapper.selectOne(new LambdaQueryWrapper<ApprovalTask>()
                .eq(ApprovalTask::getInstanceId, instanceId));
        assertThat(task.getAssigneeId()).isEqualTo(teacherUserId);

        exchange("/web/approval/tasks/" + task.getId() + "/approve?comment=ok",
                HttpMethod.POST, null, teacherToken);

        assertThat(visitorRecordMapper.selectById(recordId).getStatus()).isEqualTo(1);
        VisitorPass pass = visitorPassMapper.selectOne(new LambdaQueryWrapper<VisitorPass>()
                .eq(VisitorPass::getRecordId, recordId));
        assertThat(pass).isNotNull();
        assertThat(pass.getQrToken()).isNotBlank();
    }

    @Test
    void batchCreateIssuesPassesAgainstRealMysql() {
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
        String body = """
                {"items":[
                  {"name":"访客A","phone":"13900006001","expectedTime":"%s","leaveTime":"%s"},
                  {"name":"访客B","phone":"13900006002","expectedTime":"%s","leaveTime":"%s"}
                ]}
                """.formatted(formatter.format(LocalDateTime.now().plusDays(1)),
                formatter.format(LocalDateTime.now().plusDays(1).plusHours(2)),
                formatter.format(LocalDateTime.now().plusDays(2)),
                formatter.format(LocalDateTime.now().plusDays(2).plusHours(2)));

        Map<String, Object> result = exchange("/web/admin/visitor-records/batch",
                HttpMethod.POST, body, adminToken());

        assertThat(((Number) data(result).get("created")).intValue()).isEqualTo(2);
        assertThat(visitorRecordMapper.selectCount(null)).isEqualTo(2L);
        assertThat(visitorPassMapper.selectCount(null)).isEqualTo(2L);
    }

    @Test
    void visitorChainFromApplyToVerifyWorksAgainstRealMysql() {
        Long teacherUserId = createUserWithRole("13900007001", RoleEnum.TEACHER);
        Staff teacher = staff("张老师", "13900007001");
        insertApprovalFlowAndRule();

        Long visitorId = createUserWithRole("13900007002", RoleEnum.VISITOR);
        String visitorToken = jwtUtil.generate(visitorId, "访客", List.of(RoleEnum.VISITOR), RoleEnum.VISITOR, null);
        String teacherToken = jwtUtil.generate(teacherUserId, "老师", List.of(RoleEnum.TEACHER), RoleEnum.TEACHER, 1L);
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

        Map<String, Object> applied = data(post("/web/visitor/records", visitorToken, Map.of(
                "targetId", teacher.getId(), "visitorType", "NORMAL",
                "expectedTime", formatter.format(LocalDateTime.now()),
                "leaveTime", formatter.format(LocalDateTime.now().plusHours(2)))));
        Long recordId = ((Number) applied.get("recordId")).longValue();
        Long instanceId = ((Number) applied.get("approvalInstanceId")).longValue();

        ApprovalTask task = approvalTaskMapper.selectOne(new LambdaQueryWrapper<ApprovalTask>()
                .eq(ApprovalTask::getInstanceId, instanceId));
        exchange("/web/approval/tasks/" + task.getId() + "/approve", HttpMethod.POST, null, teacherToken);

        VisitorPass pass = visitorPassMapper.selectOne(new LambdaQueryWrapper<VisitorPass>()
                .eq(VisitorPass::getRecordId, recordId));
        pass.setValidFrom(LocalDateTime.now().minusMinutes(1));
        visitorPassMapper.updateById(pass);

        Map<String, Object> in = data(exchange("/web/guard/verify?token=" + pass.getQrToken(),
                HttpMethod.POST, null, adminToken()));
        assertThat(in.get("direction")).isEqualTo("IN");

        Map<String, Object> out = data(exchange("/web/guard/verify?token=" + pass.getQrToken(),
                HttpMethod.POST, null, adminToken()));
        assertThat(out.get("direction")).isEqualTo("OUT");
        assertThat(visitorRecordMapper.selectById(recordId).getStatus()).isEqualTo(4);
        assertThat(visitLogMapper.selectCount(null)).isEqualTo(2L);

        Map<String, Object> blockedReentry = exchange("/web/guard/verify?token=" + pass.getQrToken(),
                HttpMethod.POST, null, adminToken());
        assertThat(blockedReentry.get("code")).isEqualTo(400);
        assertThat(blockedReentry.get("message")).asString().contains("次数");

        VisitorPass multi = visitorPassMapper.selectById(pass.getId());
        multi.setMaxEntries(2);
        visitorPassMapper.updateById(multi);

        Map<String, Object> reentry = data(exchange("/web/guard/verify?token=" + pass.getQrToken(),
                HttpMethod.POST, null, adminToken()));
        assertThat(reentry.get("direction")).isEqualTo("IN");
        assertThat(visitorRecordMapper.selectById(recordId).getStatus()).isEqualTo(3);

        Map<String, Object> secondOut = data(exchange("/web/guard/verify?token=" + pass.getQrToken(),
                HttpMethod.POST, null, adminToken()));
        assertThat(secondOut.get("direction")).isEqualTo("OUT");
        assertThat(visitLogMapper.selectCount(null)).isEqualTo(4L);
    }

    @Test
    void blacklistBlocksAndWhitelistAutoApprovesAgainstRealMysql() {
        Staff teacher = staff("李老师", "13900007003");
        Long blockedId = createUserWithRole("13900007004", RoleEnum.VISITOR);
        Long whitelistId = createUserWithRole("13900007005", RoleEnum.VISITOR);
        insertList(1, "13900007004");
        insertList(2, "13900007005");
        insertApprovalFlowAndRule();
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
        String body = """
                {"targetId":%d,"visitorType":"NORMAL","expectedTime":"%s","leaveTime":"%s"}
                """.formatted(teacher.getId(), formatter.format(LocalDateTime.now()),
                formatter.format(LocalDateTime.now().plusHours(2)));

        Map<String, Object> blocked = post("/web/visitor/records",
                jwtUtil.generate(blockedId, "黑名单", List.of(RoleEnum.VISITOR), RoleEnum.VISITOR, null), body);
        assertThat(blocked.get("code")).isEqualTo(403);

        Map<String, Object> allowed = data(post("/web/visitor/records",
                jwtUtil.generate(whitelistId, "白名单", List.of(RoleEnum.VISITOR), RoleEnum.VISITOR, null), body));
        assertThat(allowed.get("approvalInstanceId")).isNull();
        assertThat(visitorRecordMapper.selectById(((Number) allowed.get("recordId")).longValue()).getStatus())
                .isEqualTo(1);
    }

    private Staff staff(String name, String phone) {
        Staff staff = new Staff();
        staff.setSchoolId(1L);
        staff.setName(name);
        staff.setPhone(phone);
        staff.setPersonType(PersonTypeEnum.TEACHER);
        staff.setStatus(1);
        staff.setSyncSource("LOCAL");
        staff.setCreatedAt(LocalDateTime.now());
        staff.setUpdatedAt(LocalDateTime.now());
        staffMapper.insert(staff);
        return staff;
    }

    private void insertList(int listType, String value) {
        AccessList list = new AccessList();
        list.setSchoolId(1L);
        list.setListType(listType);
        list.setTargetType("PERSON");
        list.setTargetValue(value);
        list.setStatus(1);
        list.setSource("MANUAL");
        list.setCreatedAt(LocalDateTime.now());
        list.setUpdatedAt(LocalDateTime.now());
        accessListMapper.insert(list);
    }
    @Test
    void requiredFormFieldBlocksApplyAgainstRealMysql() {
        Staff teacher = staff("表单老师", "13900008001");
        insertApprovalFlowAndRule();
        FormField field = new FormField();
        field.setVisitorType("NORMAL");
        field.setFieldKey("company");
        field.setLabel("工作单位");
        field.setFieldType("TEXT");
        field.setRequired(1);
        field.setStatus(1);
        formFieldMapper.insert(field);

        Long visitorId = createUserWithRole("13900008002", RoleEnum.VISITOR);
        String token = jwtUtil.generate(visitorId, "访客", List.of(RoleEnum.VISITOR), RoleEnum.VISITOR, null);
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
        Map<String, Object> body = Map.of(
                "targetId", teacher.getId(),
                "visitorType", "NORMAL",
                "expectedTime", formatter.format(LocalDateTime.now()),
                "leaveTime", formatter.format(LocalDateTime.now().plusHours(2)));

        Map<String, Object> response = post("/web/visitor/records", token, body);

        assertThat(response.get("code")).isEqualTo(400);
        assertThat(response.get("message").toString()).contains("工作单位");
    }

    @Test
    void parentSlotIsEnforcedAgainstRealMysql() {
        Staff teacher = staff("时段老师", "13900008003");
        insertApprovalFlowAndRule();
        Long visitorId = createUserWithRole("13900008004", RoleEnum.VISITOR);
        String token = jwtUtil.generate(visitorId, "家长", List.of(RoleEnum.VISITOR), RoleEnum.VISITOR, null);
        java.time.LocalDate monday = java.time.LocalDate.now()
                .with(java.time.temporal.TemporalAdjusters.next(java.time.DayOfWeek.MONDAY));

        ParentSlot slot = new ParentSlot();
        slot.setVisitorPhone("13900008004");
        slot.setWeekday(1);
        slot.setStartTime(java.time.LocalTime.of(8, 0));
        slot.setEndTime(java.time.LocalTime.of(18, 0));
        slot.setEffectiveFrom(monday.minusDays(7));
        slot.setEffectiveTo(monday.plusDays(7));
        slot.setStatus(1);
        parentSlotMapper.insert(slot);

        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
        Map<String, Object> ok = post("/web/visitor/records", token, Map.of(
                "targetId", teacher.getId(), "visitorType", "PARENT",
                "expectedTime", monday.atTime(9, 0).format(formatter),
                "leaveTime", monday.atTime(10, 0).format(formatter)));
        assertThat(ok.get("code")).isEqualTo(200);

        Map<String, Object> bad = post("/web/visitor/records", token, Map.of(
                "targetId", teacher.getId(), "visitorType", "PARENT",
                "expectedTime", monday.plusDays(1).atTime(9, 0).format(formatter),
                "leaveTime", monday.plusDays(1).atTime(10, 0).format(formatter)));
        assertThat(bad.get("code")).isEqualTo(400);
        assertThat(bad.get("message").toString()).contains("家长固定时段");
    }

    @Test
    void disabledNotifyTypeSuppressesNotificationsAgainstRealMysql() {
        Long teacherUserId = createUserWithRole("13900008005", RoleEnum.TEACHER);
        Staff teacher = staff("通知老师", "13900008005");
        insertApprovalFlowAndRule();
        NotifyConfig config = new NotifyConfig();
        config.setNotifyType("APPROVAL_RESULT");
        config.setEnabled(0);
        config.setChannels("SITE");
        notifyConfigMapper.insert(config);

        Long visitorId = createUserWithRole("13900008006", RoleEnum.VISITOR);
        String visitorToken = jwtUtil.generate(visitorId, "访客", List.of(RoleEnum.VISITOR), RoleEnum.VISITOR, null);
        String teacherToken = jwtUtil.generate(teacherUserId, "老师", List.of(RoleEnum.TEACHER), RoleEnum.TEACHER, 1L);
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
        Map<String, Object> applied = data(post("/web/visitor/records", visitorToken, Map.of(
                "targetId", teacher.getId(), "visitorType", "NORMAL",
                "expectedTime", formatter.format(LocalDateTime.now()),
                "leaveTime", formatter.format(LocalDateTime.now().plusHours(2)))));
        Long instanceId = ((Number) applied.get("approvalInstanceId")).longValue();
        ApprovalTask task = approvalTaskMapper.selectOne(new LambdaQueryWrapper<ApprovalTask>()
                .eq(ApprovalTask::getInstanceId, instanceId));
        exchange("/web/approval/tasks/" + task.getId() + "/approve", HttpMethod.POST, null, teacherToken);

        assertThat(notificationMapper.selectCount(null)).isZero();
    }
    @Test
    void overstayCreatesAlarmAgainstRealMysql() {
        createUserWithRole("13900009001", RoleEnum.SECURITY_ADMIN);
        VisitorRecord record = new VisitorRecord();
        record.setSchoolId(1L);
        record.setVisitorUserId(1L);
        record.setVisitorName("滞留访客");
        record.setVisitorType("NORMAL");
        record.setExpectedTime(LocalDateTime.now().minusHours(14));
        record.setLeaveTime(LocalDateTime.now().minusHours(13));
        record.setStatus(3);
        record.setCreatedAt(LocalDateTime.now());
        record.setUpdatedAt(LocalDateTime.now());
        visitorRecordMapper.insert(record);

        int created = alarmService.checkOverstay();

        assertThat(created).isEqualTo(1);
        assertThat(alarmRecordMapper.selectCount(null)).isEqualTo(1L);
        assertThat(notificationMapper.selectCount(null)).isGreaterThanOrEqualTo(1L);
        assertThat(alarmService.checkOverstay()).isZero();
    }
    @Test
    void gateRecordIsPulledIntoVisitLogAgainstRealMysql() {
        com.visitor.domain.entity.IntegrationConfig config = integrationConfigMapper.selectOne(
                new LambdaQueryWrapper<com.visitor.domain.entity.IntegrationConfig>()
                        .eq(com.visitor.domain.entity.IntegrationConfig::getIntegrationKey, "GATE"));
        if (config == null) {
            config = new com.visitor.domain.entity.IntegrationConfig();
            config.setIntegrationKey("GATE");
            config.setEnabled(1);
            config.setProtocol("MOCK");
            config.setStatus(1);
            config.setCreatedAt(LocalDateTime.now());
            config.setUpdatedAt(LocalDateTime.now());
            integrationConfigMapper.insert(config);
        } else {
            config.setEnabled(1);
            config.setProtocol("MOCK");
            integrationConfigMapper.updateById(config);
        }

        VisitorRecord record = new VisitorRecord();
        record.setSchoolId(1L);
        record.setVisitorUserId(1L);
        record.setVisitorName("门闸访客");
        record.setVisitorType("NORMAL");
        record.setPlateNumber("粤B77777");
        record.setExpectedTime(LocalDateTime.now().minusHours(1));
        record.setLeaveTime(LocalDateTime.now().plusHours(2));
        record.setStatus(1);
        record.setCreatedAt(LocalDateTime.now());
        record.setUpdatedAt(LocalDateTime.now());
        visitorRecordMapper.insert(record);

        mockGateClient.offer(new GateClient.GateRecord("ext-777", "粤B77777", "IN", LocalDateTime.now(), "gate-1"));

        assertThat(gateRecordIntakeService.pull()).isEqualTo(1);
        assertThat(visitorRecordMapper.selectById(record.getId()).getStatus()).isEqualTo(3);
        assertThat(visitLogMapper.selectList(null).get(0).getSource()).isEqualTo("GATE");
        assertThat(gateRecordIntakeService.pull()).isZero();
    }
    private void insertApprovalFlowAndRule() {
        approvalFlowMapper.delete(null);
        approvalRuleMapper.delete(null);
        ApprovalFlow flow = new ApprovalFlow();
        flow.setFlowCode("VISIT_DEFAULT");
        flow.setName("默认访客流程");
        flow.setVersion(1);
        flow.setStatus(1);
        flow.setDefinitionJson("{\"code\":\"VISIT_DEFAULT\",\"name\":\"默认访客流程\",\"timeoutHours\":24,"
                + "\"nodes\":[{\"seq\":1,\"name\":\"被访人审批\",\"assignee\":\"TARGET\"}]}");
        flow.setCreatedAt(LocalDateTime.now());
        flow.setUpdatedAt(LocalDateTime.now());
        approvalFlowMapper.insert(flow);

        ApprovalRule rule = new ApprovalRule();
        rule.setTargetType("TEACHER");
        rule.setFlowCode("VISIT_DEFAULT");
        rule.setPriority(10);
        rule.setStatus(1);
        rule.setCreatedAt(LocalDateTime.now());
        rule.setUpdatedAt(LocalDateTime.now());
        approvalRuleMapper.insert(rule);
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

    private void enableDataSyncMock() {
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

    private String adminToken() {
        Map<String, Object> body = login("13800000001", "admin123");
        return token(body);
    }

    private Map<String, Object> login(String phone, String password) {
        Map<String, Object> payload = new HashMap<>();
        payload.put("phone", phone);
        payload.put("password", password);
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        ResponseEntity<Map> response = rest.exchange(url("/auth/login"), HttpMethod.POST,
                new HttpEntity<>(payload, headers), Map.class);
        return response.getBody();
    }

    private Map<String, Object> get(String path, String token) {
        return exchange(path, HttpMethod.GET, null, token);
    }

    private Map<String, Object> post(String path, String token, Object payload) {
        return exchange(path, HttpMethod.POST, payload, token);
    }

    @SuppressWarnings("unchecked")
    private Map<String, Object> exchange(String path, HttpMethod method, Object payload, String token) {
        HttpHeaders headers = bearer(token);
        headers.setContentType(MediaType.APPLICATION_JSON);
        ResponseEntity<Map> response = rest.exchange(url(path), method,
                new HttpEntity<>(payload, headers), Map.class);
        return response.getBody();
    }

    @SuppressWarnings("unchecked")
    private Map<String, Object> data(Map<String, Object> body) {
        return (Map<String, Object>) body.get("data");
    }

    private String token(Map<String, Object> body) {
        return String.valueOf(data(body).get("token"));
    }

    private HttpHeaders bearer(String token) {
        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(token);
        return headers;
    }

    private String url(String path) {
        return "http://localhost:" + port + "/api" + path;
    }
}
