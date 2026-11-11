package com.visitor.server.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.visitor.common.core.PageResult;
import com.visitor.common.exception.BizException;
import com.visitor.domain.dto.VisitorApplyDTO;
import com.visitor.domain.dto.VisitorBatchDTO;
import com.visitor.domain.entity.ApprovalInstance;
import com.visitor.domain.entity.Org;
import com.visitor.domain.entity.Staff;
import com.visitor.domain.entity.User;
import com.visitor.domain.entity.VisitorPass;
import com.visitor.domain.entity.VisitorRecord;
import com.visitor.server.mapper.ApprovalInstanceMapper;
import com.visitor.server.mapper.OrgMapper;
import com.visitor.server.mapper.ApprovalTaskMapper;
import com.visitor.domain.entity.ApprovalTask;
import com.visitor.server.mapper.StaffMapper;
import com.visitor.server.mapper.UserMapper;
import com.visitor.server.mapper.VisitorPassMapper;
import com.visitor.server.mapper.VisitorRecordMapper;
import com.visitor.server.service.VisitorService;
import com.visitor.server.service.NotificationService;
import com.visitor.server.service.FormFieldService;
import com.visitor.server.service.ParentSlotService;
import com.visitor.server.service.AlarmService;
import com.visitor.server.service.AuditLogService;
import com.visitor.server.service.SystemSettingService;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.visitor.server.service.AccessCheckService;
import com.visitor.common.enums.PersonTypeEnum;
import com.visitor.common.enums.RoleEnum;
import com.visitor.security.RoleAccessChecker;
import com.visitor.security.UserContext;
import com.visitor.workflow.ApprovalEngine;
import com.visitor.workflow.ApprovalStatuses;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;

@Service
public class VisitorServiceImpl implements VisitorService {

    private final UserMapper userMapper;
    private final OrgMapper orgMapper;
    private final StaffMapper staffMapper;
    private final VisitorRecordMapper recordMapper;
    private final VisitorPassMapper passMapper;
    private final VisitorPassIssuer passIssuer;
    private final ApprovalEngine approvalEngine;
    private final ApprovalInstanceMapper instanceMapper;
    private final ApprovalTaskMapper taskMapper;
    private final AccessCheckService accessCheckService;
    private final NotificationService notificationService;
    private final FormFieldService formFieldService;
    private final ParentSlotService parentSlotService;
    private final AlarmService alarmService;
    private final AuditLogService auditLogService;
    private final SystemSettingService settingService;
    private final ObjectMapper objectMapper;
    private final RoleAccessChecker roleAccessChecker;

    public VisitorServiceImpl(UserMapper userMapper, OrgMapper orgMapper, StaffMapper staffMapper,
                              VisitorRecordMapper recordMapper, VisitorPassMapper passMapper,
                              VisitorPassIssuer passIssuer, ApprovalEngine approvalEngine,
                              ApprovalInstanceMapper instanceMapper, ApprovalTaskMapper taskMapper, AccessCheckService accessCheckService, NotificationService notificationService, FormFieldService formFieldService, ParentSlotService parentSlotService, AlarmService alarmService, AuditLogService auditLogService, SystemSettingService settingService, ObjectMapper objectMapper, RoleAccessChecker roleAccessChecker) {
        this.userMapper = userMapper;
        this.orgMapper = orgMapper;
        this.staffMapper = staffMapper;
        this.recordMapper = recordMapper;
        this.passMapper = passMapper;
        this.passIssuer = passIssuer;
        this.approvalEngine = approvalEngine;
        this.instanceMapper = instanceMapper;
        this.taskMapper = taskMapper;
        this.accessCheckService = accessCheckService;
        this.notificationService = notificationService;
        this.formFieldService = formFieldService;
        this.parentSlotService = parentSlotService;
        this.alarmService = alarmService;
        this.auditLogService = auditLogService;
        this.settingService = settingService;
        this.objectMapper = objectMapper;
        this.roleAccessChecker = roleAccessChecker;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Map<String, Object> apply(Long userId, VisitorApplyDTO dto) {
        User user = userMapper.selectById(userId);
        if (user == null) {
            throw new BizException(401, "用户不存在");
        }
        Staff target = staffMapper.selectById(dto.getTargetId());
        if (target == null || !Integer.valueOf(1).equals(target.getStatus())) {
            throw new BizException(400, "被访人不存在或已停用");
        }
        validateTime(dto.getExpectedTime(), dto.getLeaveTime());
        String plate = dto.getPlateNumber() == null ? null : dto.getPlateNumber().trim().toUpperCase();
        String access = accessCheckService.check(target.getSchoolId(), user.getPhone(), plate);
        if ("BLACKLIST".equals(access)) {
            alarmService.notifyBlacklistHit(user.getName(), user.getPhone());
            auditLogService.record("BLACKLIST_HIT", "ACCESS_LIST", null, "黑名单拦截：" + user.getPhone());
            throw new BizException(403, "该访客已被列入黑名单，禁止入校");
        }
        if (settingService.getBool("visit.duplicate_check_enabled", true)) {
            assertNoOverlap(userId, dto.getExpectedTime(), dto.getLeaveTime());
        }

        formFieldService.validate(dto.getVisitorType() == null || dto.getVisitorType().isBlank()
                ? "NORMAL" : dto.getVisitorType().trim().toUpperCase(), dto.getExtra());

        String visitorType = dto.getVisitorType() == null || dto.getVisitorType().isBlank()
                ? "NORMAL" : dto.getVisitorType().trim().toUpperCase();

        if ("PARENT".equals(visitorType)) {
            parentSlotService.validate(user.getPhone(), dto.getExpectedTime(), dto.getLeaveTime());
        }

        String passMode = dto.getPassMode() == null || dto.getPassMode().isBlank()
                ? "WALK" : dto.getPassMode().trim().toUpperCase();
        if (!settingService.getList("visit.pass_modes", "WALK,EBIKE,CAR").contains(passMode)) {
            throw new BizException(400, "通行方式不合法");
        }
        if ("CAR".equals(passMode) && (dto.getPlateNumber() == null || dto.getPlateNumber().isBlank())) {
            throw new BizException(400, "机动车通行请填写车牌号");
        }

        VisitorRecord record = new VisitorRecord();
        record.setSchoolId(target.getSchoolId());
        record.setVisitorUserId(userId);
        record.setVisitorName(user.getName() == null ? "访客" : user.getName());
        record.setVisitorPhone(user.getPhone());
        record.setIdCard(dto.getIdCard());
        record.setPlateNumber(dto.getPlateNumber() == null ? null : dto.getPlateNumber().trim().toUpperCase());
        record.setPassMode(passMode);

        Long assigneeHint = null;
        if (target.getPersonType() != null && "AFFILIATED_STUDENT".equals(target.getPersonType().name())) {
            if (dto.getHeadTeacherNo() == null || dto.getHeadTeacherNo().isBlank()) {
                throw new BizException(400, "附属学生预约请填写班主任工号");
            }
            Staff headTeacher = staffMapper.selectOne(new LambdaQueryWrapper<Staff>()
                    .eq(Staff::getJobNo, dto.getHeadTeacherNo().trim())
                    .eq(Staff::getStatus, 1)
                    .last("LIMIT 1"));
            if (headTeacher == null) {
                throw new BizException(400, "班主任工号不存在");
            }
            User headTeacherUser = headTeacher.getPhone() == null ? null
                    : userMapper.selectOne(new LambdaQueryWrapper<User>()
                    .eq(User::getPhone, headTeacher.getPhone()).last("LIMIT 1"));
            if (headTeacherUser == null) {
                throw new BizException(400, "班主任未绑定系统账号");
            }
            assigneeHint = headTeacherUser.getId();
            record.setHeadTeacherNo(dto.getHeadTeacherNo().trim());
        }
        record.setVisitorType(visitorType);
        record.setTargetType(target.getPersonType() == null ? null : target.getPersonType().name());
        record.setTargetId(target.getId());
        record.setTargetName(target.getName());
        record.setVisitPurpose(dto.getVisitPurpose());
        record.setExtraJson(writeExtra(dto.getExtra()));
        record.setExpectedTime(dto.getExpectedTime());
        record.setLeaveTime(dto.getLeaveTime());
        boolean autoApprove = "WHITELIST".equals(access) || "ALUMNI".equals(visitorType);
        record.setStatus(autoApprove ? 1 : 0);
        record.setCreatedAt(LocalDateTime.now());
        record.setUpdatedAt(LocalDateTime.now());
        recordMapper.insert(record);
        User targetUser = target.getPhone() == null ? null : userMapper.selectOne(
                new LambdaQueryWrapper<User>().eq(User::getPhone, target.getPhone()));
        boolean affiliatedStudent = target.getPersonType() == PersonTypeEnum.AFFILIATED_STUDENT;
        // 附属学生不参与审批，审批人是工号对应的班主任，避免给学生发“请及时审批”
        if (targetUser != null && !affiliatedStudent) {
            notificationService.notifyUser(targetUser.getId(), "新的访客申请",
                    record.getVisitorName() + " 申请入校，请及时审批", "APPROVAL_RESULT",
                    "VISITOR_RECORD", record.getId());
        }

        ApprovalInstance instance;
        if (autoApprove) {
            passIssuer.issue(record);
            instance = null;
        } else {
            instance = approvalEngine.start(new ApprovalEngine.StartCommand(
                    "VISITOR_RECORD", record.getId(), record.getSchoolId(), userId,
                    visitorType, record.getTargetType(), record.getTargetId(), null, assigneeHint));
        }

        auditLogService.record("VISITOR_APPLY", "VISITOR_RECORD", record.getId(),
                "提交预约：" + record.getVisitorName() + " → " + record.getTargetName());

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("recordId", record.getId());
        result.put("approvalInstanceId", instance == null ? null : instance.getId());
        result.put("status", record.getStatus());
        return result;
    }

    @Override
    public PageResult<VisitorRecord> myRecords(Long userId, int page, int size) {
        int safePage = Math.max(page, 1);
        int safeSize = Math.min(Math.max(size, 1), 100);
        Page<VisitorRecord> result = recordMapper.selectPage(new Page<>(safePage, safeSize),
                new LambdaQueryWrapper<VisitorRecord>()
                        .eq(VisitorRecord::getVisitorUserId, userId)
                        .orderByDesc(VisitorRecord::getId));
        return PageResult.of(result.getTotal(), safePage, safeSize, result.getRecords());
    }

    @Override
    public VisitorRecord detail(Long userId, Long id) {
        VisitorRecord record = recordMapper.selectById(id);
        if (record == null || !Objects.equals(record.getVisitorUserId(), userId)) {
            throw new BizException(404, "记录不存在");
        }
        return record;
    }

    @Override
    public VisitorPass pass(Long userId, Long recordId) {
        detail(userId, recordId);
        VisitorPass pass = passMapper.selectOne(new LambdaQueryWrapper<VisitorPass>()
                .eq(VisitorPass::getRecordId, recordId));
        if (pass == null) {
            throw new BizException(404, "凭证尚未生成");
        }
        return pass;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void cancel(Long userId, Long id) {
        VisitorRecord record = detail(userId, id);
        if (record.getStatus() != 0 && record.getStatus() != 1) {
            throw new BizException(400, "当前状态不可取消");
        }
        record.setStatus(5);
        record.setUpdatedAt(LocalDateTime.now());
        recordMapper.updateById(record);
        passIssuer.revoke(record.getId());

        ApprovalInstance instance = instanceMapper.selectOne(new LambdaQueryWrapper<ApprovalInstance>()
                .eq(ApprovalInstance::getBizType, "VISITOR_RECORD")
                .eq(ApprovalInstance::getBizId, record.getId()));
        if (instance != null && ApprovalStatuses.RUNNING.equals(instance.getStatus())) {
            approvalEngine.cancel(instance.getId(), "访客取消");
        }
    }

    @Override
    public PageResult<VisitorRecord> adminPage(Long schoolId, Integer status, String keyword,
                                               int page, int size) {
        int safePage = Math.max(page, 1);
        int safeSize = Math.min(Math.max(size, 1), 100);
        Page<VisitorRecord> result = recordMapper.selectPage(new Page<>(safePage, safeSize),
                new LambdaQueryWrapper<VisitorRecord>()
                        .eq(VisitorRecord::getSchoolId, schoolId)
                        .eq(status != null, VisitorRecord::getStatus, status)
                        .and(keyword != null && !keyword.isBlank(), w -> w
                                .like(VisitorRecord::getVisitorName, keyword)
                                .or().like(VisitorRecord::getVisitorPhone, keyword)
                                .or().like(VisitorRecord::getTargetName, keyword))
                        .orderByDesc(VisitorRecord::getId));
        return PageResult.of(result.getTotal(), safePage, safeSize, result.getRecords());
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public int batchCreate(Long schoolId, VisitorBatchDTO dto, Long operatorId) {
        int created = 0;
        for (VisitorBatchDTO.Item item : dto.getItems()) {
            validateTime(item.getExpectedTime(), item.getLeaveTime());
            VisitorRecord record = new VisitorRecord();
            record.setSchoolId(schoolId);
            record.setVisitorUserId(operatorId);
            record.setVisitorName(item.getName().trim());
            record.setVisitorPhone(item.getPhone());
            record.setIdCard(item.getIdCard());
            record.setVisitorType("NORMAL");
            record.setVisitPurpose(item.getVisitPurpose());
            record.setExpectedTime(item.getExpectedTime());
            record.setLeaveTime(item.getLeaveTime());
            record.setStatus(1);
            record.setCreatedAt(LocalDateTime.now());
            record.setUpdatedAt(LocalDateTime.now());
            recordMapper.insert(record);
            passIssuer.issue(record);
            created++;
        }
        return created;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public int deptBatch(Long schoolId, VisitorBatchDTO dto, Long operatorId) {
        if (!roleAccessChecker.hasAny(RoleEnum.DEPT_OPERATOR, RoleEnum.SECURITY_ADMIN, RoleEnum.SYSTEM_ADMIN)) {
            throw new BizException(403, "仅部门经办人或保卫处管理员可发起批量预约");
        }
        int created = 0;
        for (VisitorBatchDTO.Item item : dto.getItems()) {
            validateTime(item.getExpectedTime(), item.getLeaveTime());
            VisitorRecord record = new VisitorRecord();
            record.setSchoolId(schoolId);
            record.setVisitorUserId(operatorId);
            record.setVisitorName(item.getName().trim());
            record.setVisitorPhone(item.getPhone());
            record.setIdCard(item.getIdCard());
            record.setVisitorType("NORMAL");
            record.setVisitPurpose(item.getVisitPurpose());
            record.setExpectedTime(item.getExpectedTime());
            record.setLeaveTime(item.getLeaveTime());
            record.setStatus(0);
            record.setCreatedAt(LocalDateTime.now());
            record.setUpdatedAt(LocalDateTime.now());
            recordMapper.insert(record);
            approvalEngine.start(new ApprovalEngine.StartCommand(
                    "VISITOR_RECORD", record.getId(), schoolId, operatorId,
                    "NORMAL", null, null, "VISIT_DEPT_BATCH"));
            created++;
        }
        return created;
    }

    @Override
    public java.util.Map<String, Object> approvalInfo(Long userId, Long recordId) {
        detail(userId, recordId);
        ApprovalInstance instance = instanceMapper.selectOne(new LambdaQueryWrapper<ApprovalInstance>()
                .eq(ApprovalInstance::getBizType, "VISITOR_RECORD")
                .eq(ApprovalInstance::getBizId, recordId)
                .last("LIMIT 1"));
        if (instance == null) {
            return java.util.Map.of();
        }
        ApprovalTask task = taskMapper.selectOne(new LambdaQueryWrapper<ApprovalTask>()
                .eq(ApprovalTask::getInstanceId, instance.getId())
                .isNotNull(ApprovalTask::getActedAt)
                .orderByDesc(ApprovalTask::getActedAt)
                .last("LIMIT 1"));
        if (task == null) {
            return java.util.Map.of();
        }
        java.util.Map<String, Object> body = new java.util.LinkedHashMap<>();
        body.put("decision", task.getDecision());
        body.put("comment", task.getComment());
        body.put("nodeName", task.getNodeName());
        body.put("actedAt", task.getActedAt());
        return body;
    }

    @Override
    public java.util.Map<String, Object> preCheck(Long userId, String plate) {
        User user = userMapper.selectById(userId);
        if (user == null) {
            throw new BizException(401, "用户不存在");
        }
        String result = accessCheckService.check(1L, user.getPhone(), plate);
        java.util.Map<String, Object> body = new java.util.LinkedHashMap<>();
        body.put("status", result);
        body.put("message", switch (result) {
            case "BLACKLIST" -> "您在黑名单中，无法提交预约";
            case "WHITELIST" -> "您在白名单中，可免审批通行";
            default -> "";
        });
        return body;
    }

    @Override
    public java.util.List<com.visitor.domain.vo.VisitorRecordExportRow> exportRows(Integer status, String keyword) {
        java.util.List<VisitorRecord> records = recordMapper.selectList(new LambdaQueryWrapper<VisitorRecord>()
                .eq(status != null, VisitorRecord::getStatus, status)
                .and(keyword != null && !keyword.isBlank(), w -> w
                        .like(VisitorRecord::getVisitorName, keyword)
                        .or().like(VisitorRecord::getVisitorPhone, keyword)
                        .or().like(VisitorRecord::getTargetName, keyword))
                .orderByDesc(VisitorRecord::getId)
                .last("LIMIT 5000"));
        java.time.format.DateTimeFormatter formatter = java.time.format.DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
        java.util.List<com.visitor.domain.vo.VisitorRecordExportRow> rows = new java.util.ArrayList<>();
        for (VisitorRecord record : records) {
            com.visitor.domain.vo.VisitorRecordExportRow row = new com.visitor.domain.vo.VisitorRecordExportRow();
            row.setVisitorName(record.getVisitorName());
            row.setVisitorPhone(record.getVisitorPhone());
            row.setIdCard(record.getIdCard());
            row.setTargetName(record.getTargetName());
            row.setVisitPurpose(record.getVisitPurpose());
            row.setPassMode(switch (record.getPassMode() == null ? "WALK" : record.getPassMode()) {
                case "CAR" -> "机动车";
                case "EBIKE" -> "电动自行车";
                default -> "步行";
            });
            row.setPlateNumber(record.getPlateNumber());
            row.setExpectedTime(record.getExpectedTime() == null ? null : record.getExpectedTime().format(formatter));
            row.setLeaveTime(record.getLeaveTime() == null ? null : record.getLeaveTime().format(formatter));
            row.setStatusText(STATUS_TEXT.getOrDefault(record.getStatus(), String.valueOf(record.getStatus())));
            rows.add(row);
        }
        return rows;
    }

    private static final java.util.Map<Integer, String> STATUS_TEXT = java.util.Map.of(
            0, "待审批", 1, "已通过", 2, "已拒绝", 3, "已入校", 4, "已离校", 5, "已取消", 6, "已过期");

    @Override
    public java.util.List<Staff> queryTargets(String keyword) {
        if (keyword == null || keyword.isBlank()) {
            return java.util.List.of();
        }
        String value = keyword.trim();
        java.util.List<Staff> staffList = staffMapper.selectList(new LambdaQueryWrapper<Staff>()
                .eq(Staff::getStatus, 1)
                .eq(Staff::getIsDeleted, 0)
                .and(w -> w.eq(Staff::getName, value).or().eq(Staff::getPhone, value))
                .orderByAsc(Staff::getId)
                .last("LIMIT 10"));
        if (staffList.isEmpty()) {
            return staffList;
        }
        for (Staff staff : staffList) {
            Org org = staff.getOrgId() == null ? null : orgMapper.selectById(staff.getOrgId());
            staff.setOrgName(org == null ? null : org.getName());
        }
        return staffList;
    }

    @Override
    public java.util.List<VisitorRecord> queryByPlate(String plate) {
        if (!roleAccessChecker.hasAny(RoleEnum.SECURITY, RoleEnum.SECURITY_ADMIN, RoleEnum.SYSTEM_ADMIN)) {
            throw new BizException(403, "无权查询车主信息");
        }
        if (plate == null || plate.isBlank()) {
            return java.util.List.of();
        }
        return recordMapper.selectList(new LambdaQueryWrapper<VisitorRecord>()
                .eq(VisitorRecord::getPlateNumber, plate.trim().toUpperCase())
                .in(VisitorRecord::getStatus, 1, 3, 4)
                .orderByDesc(VisitorRecord::getExpectedTime)
                .last("LIMIT 5"));
    }

    @Transactional(rollbackFor = Exception.class)
    public int expireOverdue() {
        for (VisitorRecord record : recordMapper.selectList(new LambdaQueryWrapper<VisitorRecord>()
                .in(VisitorRecord::getStatus, 0, 1, 2)
                .lt(VisitorRecord::getLeaveTime, LocalDateTime.now()))) {
            if (record.getStatus() == 1) {
                passIssuer.expire(record.getId());
            }
        }
        VisitorRecord update = new VisitorRecord();
        update.setStatus(6);
        update.setUpdatedAt(LocalDateTime.now());
        return recordMapper.update(update, new LambdaQueryWrapper<VisitorRecord>()
                .in(VisitorRecord::getStatus, 0, 1, 2)
                .lt(VisitorRecord::getLeaveTime, LocalDateTime.now()));
    }

    private String writeExtra(Map<String, String> extra) {
        if (extra == null || extra.isEmpty()) {
            return null;
        }
        try {
            return objectMapper.writeValueAsString(extra);
        } catch (Exception e) {
            throw new BizException(400, "表单数据格式错误");
        }
    }

    private void validateTime(LocalDateTime expected, LocalDateTime leave) {
        if (expected == null || leave == null) {
            throw new BizException(400, "到访时间不能为空");
        }
        if (!leave.isAfter(expected)) {
            throw new BizException(400, "离校时间必须晚于到访时间");
        }
        if (settingService.getBool("visit.unusual_hours_enabled", false)) {
            java.time.LocalTime start = java.time.LocalTime.parse(settingService.get("visit.unusual_hours_start", "07:00"));
            java.time.LocalTime end = java.time.LocalTime.parse(settingService.get("visit.unusual_hours_end", "22:00"));
            java.time.LocalTime expectedTime = expected.toLocalTime();
            if (expectedTime.isBefore(start) || expectedTime.isAfter(end)) {
                throw new BizException(400, "非常规时段暂不可预约（允许 " + start + "-" + end + "）");
            }
        }
        if (leave.isBefore(LocalDateTime.now())) {
            throw new BizException(400, "离校时间已过");
        }
    }

    private void assertNoOverlap(Long userId, LocalDateTime expected, LocalDateTime leave) {
        Long overlap = recordMapper.selectCount(new LambdaQueryWrapper<VisitorRecord>()
                .eq(VisitorRecord::getVisitorUserId, userId)
                .in(VisitorRecord::getStatus, 0, 1)
                .lt(VisitorRecord::getExpectedTime, leave)
                .gt(VisitorRecord::getLeaveTime, expected));
        if (overlap != null && overlap > 0) {
            throw new BizException(400, "同一时段已有预约，请勿重复提交");
        }
    }
}
