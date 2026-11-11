package com.visitor.workflow;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.visitor.common.core.PageResult;
import com.visitor.common.enums.RoleEnum;
import com.visitor.common.exception.BizException;
import com.visitor.domain.entity.ApprovalFlow;
import com.visitor.domain.entity.ApprovalInstance;
import com.visitor.domain.entity.ApprovalRule;
import com.visitor.domain.entity.ApprovalTask;
import com.visitor.security.RoleAccessChecker;
import com.visitor.security.UserContext;
import com.visitor.server.service.AuditLogService;
import com.visitor.server.service.NotificationService;
import com.visitor.server.mapper.ApprovalFlowMapper;
import com.visitor.server.mapper.ApprovalInstanceMapper;
import com.visitor.server.mapper.ApprovalRuleMapper;
import com.visitor.server.mapper.ApprovalTaskMapper;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;

@Service
public class ApprovalEngineImpl implements ApprovalEngine {

    private static final String DEFAULT_FLOW = "VISIT_DEFAULT";

    private final ApprovalFlowMapper flowMapper;
    private final ApprovalRuleMapper ruleMapper;
    private final ApprovalInstanceMapper instanceMapper;
    private final ApprovalTaskMapper taskMapper;
    private final AssigneeResolver assigneeResolver;
    private final ObjectMapper objectMapper;
    private final ApplicationEventPublisher eventPublisher;
    private final AuditLogService auditLogService;
    private final NotificationService notificationService;
    private final RoleAccessChecker roleAccessChecker;

    public ApprovalEngineImpl(ApprovalFlowMapper flowMapper, ApprovalRuleMapper ruleMapper,
                              ApprovalInstanceMapper instanceMapper, ApprovalTaskMapper taskMapper,
                              AssigneeResolver assigneeResolver, ObjectMapper objectMapper,
                              ApplicationEventPublisher eventPublisher, AuditLogService auditLogService,
                              NotificationService notificationService, RoleAccessChecker roleAccessChecker) {
        this.flowMapper = flowMapper;
        this.ruleMapper = ruleMapper;
        this.instanceMapper = instanceMapper;
        this.taskMapper = taskMapper;
        this.assigneeResolver = assigneeResolver;
        this.objectMapper = objectMapper;
        this.eventPublisher = eventPublisher;
        this.auditLogService = auditLogService;
        this.notificationService = notificationService;
        this.roleAccessChecker = roleAccessChecker;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public ApprovalInstance start(StartCommand command) {
        List<ApprovalInstance> existing = instanceMapper.selectList(new LambdaQueryWrapper<ApprovalInstance>()
                .eq(ApprovalInstance::getBizType, command.bizType())
                .eq(ApprovalInstance::getBizId, command.bizId())
                .last("LIMIT 1"));
        if (!existing.isEmpty()) {
            return existing.get(0);
        }

        String flowCode = command.flowCode() == null || command.flowCode().isBlank()
                ? resolveFlowCode(command) : command.flowCode();
        ApprovalFlow flow = publishedFlow(flowCode);
        if (flow == null) {
            throw new BizException(400, "审批流程未发布: " + flowCode);
        }
        FlowDefinition definition = read(flow.getDefinitionJson());

        LocalDateTime now = LocalDateTime.now();
        ApprovalInstance instance = new ApprovalInstance();
        instance.setBizType(command.bizType());
        instance.setBizId(command.bizId());
        instance.setSchoolId(command.schoolId());
        instance.setFlowCode(flow.getFlowCode());
        instance.setFlowVersion(flow.getVersion());
        instance.setDefinitionJson(flow.getDefinitionJson());
        instance.setAssigneeHint(command.assigneeHint());
        instance.setStatus(ApprovalStatuses.RUNNING);
        instance.setApplicantId(command.applicantId());
        instance.setVisitorType(command.visitorType());
        instance.setTargetType(command.targetType());
        instance.setTargetId(command.targetId());
        instance.setStartedAt(now);
        instance.setCreatedAt(now);
        instance.setUpdatedAt(now);
        instanceMapper.insert(instance);

        FlowDefinition.Node first = nextNode(definition, 0);
        if (first == null) {
            complete(instance, ApprovalStatuses.APPROVED);
        } else {
            createTask(instance, definition, first);
        }
        return instance;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public ApprovalTask approve(Long taskId, String comment, boolean emergency) {
        ApprovalTask task = pendingTask(taskId);
        ApprovalInstance instance = runningInstance(task.getInstanceId());
        authorize(task, instance, emergency);

        task.setStatus(ApprovalStatuses.APPROVED);
        task.setDecision("APPROVED");
        task.setComment(comment);
        task.setEmergency(emergency ? 1 : 0);
        task.setActedAt(LocalDateTime.now());
        task.setActedBy(UserContext.getUserId());
        task.setUpdatedAt(LocalDateTime.now());
        taskMapper.updateById(task);
        auditLogService.record("APPROVAL_APPROVE", "APPROVAL_TASK", task.getId(), "通过审批");
        notifyProxyHandled(task, true, comment);

        FlowDefinition definition = read(instance.getDefinitionJson());
        FlowDefinition.Node next = nextNode(definition, task.getSeq());
        if (next == null) {
            complete(instance, ApprovalStatuses.APPROVED);
        } else {
            createTask(instance, definition, next);
        }
        return task;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public ApprovalTask reject(Long taskId, String comment, boolean emergency) {
        ApprovalTask task = pendingTask(taskId);
        ApprovalInstance instance = runningInstance(task.getInstanceId());
        authorize(task, instance, emergency);

        task.setStatus(ApprovalStatuses.REJECTED);
        task.setDecision("REJECTED");
        task.setComment(comment);
        task.setEmergency(emergency ? 1 : 0);
        task.setActedAt(LocalDateTime.now());
        task.setActedBy(UserContext.getUserId());
        task.setUpdatedAt(LocalDateTime.now());
        taskMapper.updateById(task);
        auditLogService.record("APPROVAL_REJECT", "APPROVAL_TASK", task.getId(), "驳回审批");
        notifyProxyHandled(task, false, comment);

        skipPending(instance.getId());
        complete(instance, ApprovalStatuses.REJECTED);
        return task;
    }

    @Override
    public List<ApprovalTask> batchApprove(List<Long> taskIds, String comment) {
        List<ApprovalTask> approved = new ArrayList<>();
        if (taskIds == null) {
            return approved;
        }
        for (Long taskId : taskIds) {
            try {
                approved.add(approve(taskId, comment, false));
            } catch (BizException ignored) {
                // 单条失败不影响其它任务
            }
        }
        return approved;
    }

    @Override
    public List<ApprovalTask> batchReject(List<Long> taskIds, String comment) {
        List<ApprovalTask> rejected = new ArrayList<>();
        if (taskIds == null) {
            return rejected;
        }
        for (Long taskId : taskIds) {
            try {
                rejected.add(reject(taskId, comment, false));
            } catch (BizException ignored) {
                // 单条失败不影响其它任务
            }
        }
        return rejected;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void cancel(Long instanceId, String reason) {
        ApprovalInstance instance = instanceMapper.selectById(instanceId);
        if (instance == null) {
            throw new BizException(404, "审批实例不存在");
        }
        if (!Objects.equals(instance.getApplicantId(), UserContext.getUserId())) {
            checkSchoolScope(instance.getSchoolId());
        }
        if (!ApprovalStatuses.RUNNING.equals(instance.getStatus())) {
            throw new BizException(400, "审批已结束，无法取消");
        }
        skipPending(instance.getId());
        instance.setStatus(ApprovalStatuses.CANCELLED);
        instance.setEndedAt(LocalDateTime.now());
        instance.setUpdatedAt(LocalDateTime.now());
        instanceMapper.updateById(instance);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public int expireOverdueTasks() {
        List<ApprovalTask> overdue = taskMapper.selectList(new LambdaQueryWrapper<ApprovalTask>()
                .eq(ApprovalTask::getStatus, ApprovalStatuses.PENDING)
                .lt(ApprovalTask::getDueAt, LocalDateTime.now())
                .last("LIMIT 200"));
        for (ApprovalTask task : overdue) {
            task.setStatus(ApprovalStatuses.EXPIRED);
            task.setDecision("EXPIRED");
            task.setComment("超时未处理，自动驳回");
            task.setActedAt(LocalDateTime.now());
            task.setUpdatedAt(LocalDateTime.now());
            taskMapper.updateById(task);

            ApprovalInstance instance = instanceMapper.selectById(task.getInstanceId());
            if (instance != null && ApprovalStatuses.RUNNING.equals(instance.getStatus())) {
                skipPending(instance.getId());
                complete(instance, ApprovalStatuses.REJECTED);
            }
        }
        return overdue.size();
    }

    @Override
    public PageResult<ApprovalTask> myTasks(int page, int size) {
        Long userId = UserContext.getUserId();
        int safePage = Math.max(page, 1);
        int safeSize = Math.min(Math.max(size, 1), 100);
        boolean admin = roleAccessChecker.hasAny(RoleEnum.SECURITY_ADMIN, RoleEnum.SYSTEM_ADMIN);
        LambdaQueryWrapper<ApprovalTask> query = new LambdaQueryWrapper<ApprovalTask>()
                .eq(ApprovalTask::getStatus, ApprovalStatuses.PENDING)
                .and(admin, wrapper -> wrapper
                        .eq(ApprovalTask::getAssigneeId, userId)
                        .or()
                        .eq(ApprovalTask::getAssigneeType, "SECURITY"))
                .eq(!admin, ApprovalTask::getAssigneeId, userId)
                .orderByAsc(ApprovalTask::getDueAt);
        Page<ApprovalTask> result = taskMapper.selectPage(new Page<>(safePage, safeSize), query);
        return PageResult.of(result.getTotal(), safePage, safeSize, result.getRecords());
    }

    @Override
    public PageResult<ApprovalInstance> instances(Long schoolId, String status, int page, int size) {
        int safePage = Math.max(page, 1);
        int safeSize = Math.min(Math.max(size, 1), 100);
        Page<ApprovalInstance> result = instanceMapper.selectPage(new Page<>(safePage, safeSize),
                new LambdaQueryWrapper<ApprovalInstance>()
                        .eq(schoolId != null, ApprovalInstance::getSchoolId, schoolId)
                        .eq(status != null && !status.isBlank(), ApprovalInstance::getStatus, status)
                        .orderByDesc(ApprovalInstance::getId));
        return PageResult.of(result.getTotal(), safePage, safeSize, result.getRecords());
    }

    @Override
    public PageResult<java.util.Map<String, Object>> history(int page, int size) {
        Long me = UserContext.getUserId();
        int safePage = Math.max(page, 1);
        int safeSize = Math.min(Math.max(size, 1), 100);
        Page<ApprovalTask> result = taskMapper.selectPage(new Page<>(safePage, safeSize),
                new LambdaQueryWrapper<ApprovalTask>()
                        .eq(ApprovalTask::getActedBy, me)
                        .ne(ApprovalTask::getStatus, ApprovalStatuses.PENDING)
                        .orderByDesc(ApprovalTask::getActedAt));
        java.util.List<java.util.Map<String, Object>> rows = new java.util.ArrayList<>();
        for (ApprovalTask task : result.getRecords()) {
            java.util.Map<String, Object> row = new java.util.LinkedHashMap<>();
            row.put("taskId", task.getId());
            row.put("instanceId", task.getInstanceId());
            row.put("nodeName", task.getNodeName());
            row.put("decision", task.getDecision());
            row.put("comment", task.getComment());
            row.put("actedAt", task.getActedAt());
            rows.add(row);
        }
        return PageResult.of(result.getTotal(), safePage, safeSize, rows);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void emergencyHandle(Long instanceId, boolean approve, String comment) {
        ApprovalInstance instance = runningInstance(instanceId);
        ApprovalTask task = taskMapper.selectOne(new LambdaQueryWrapper<ApprovalTask>()
                .eq(ApprovalTask::getInstanceId, instanceId)
                .eq(ApprovalTask::getStatus, ApprovalStatuses.PENDING)
                .orderByAsc(ApprovalTask::getSeq)
                .last("LIMIT 1"));
        if (task == null) {
            throw new BizException(400, "当前没有待处理任务");
        }
        if (approve) {
            approve(task.getId(), comment, true);
        } else {
            reject(task.getId(), comment, true);
        }
    }
    private String resolveFlowCode(StartCommand command) {
        if (command.targetType() != null) {
            List<ApprovalFlow> flows = flowMapper.selectList(new LambdaQueryWrapper<ApprovalFlow>()
                    .eq(ApprovalFlow::getStatus, 1)
                    .orderByDesc(ApprovalFlow::getVersion));
            for (ApprovalFlow flow : flows) {
                try {
                    FlowDefinition definition = read(flow.getDefinitionJson());
                    if (definition.targetTypes() != null
                            && definition.targetTypes().contains(command.targetType())) {
                        return flow.getFlowCode();
                    }
                } catch (Exception ignored) {
                    // 跳过损坏的历史流程定义，继续匹配
                }
            }
        }
        List<ApprovalRule> rules = ruleMapper.selectList(new LambdaQueryWrapper<ApprovalRule>()
                .eq(ApprovalRule::getStatus, 1)
                .orderByAsc(ApprovalRule::getPriority));
        for (ApprovalRule rule : rules) {
            boolean schoolMatch = rule.getSchoolId() == null || rule.getSchoolId().equals(command.schoolId());
            boolean visitorMatch = rule.getVisitorType() == null || rule.getVisitorType().equals(command.visitorType());
            boolean targetMatch = rule.getTargetType() == null || rule.getTargetType().equals(command.targetType());
            if (schoolMatch && visitorMatch && targetMatch) {
                return rule.getFlowCode();
            }
        }
        if (command.targetType() != null) {
            return "VISIT_" + command.targetType();
        }
        return DEFAULT_FLOW;
    }

    private ApprovalFlow publishedFlow(String flowCode) {
        return flowMapper.selectOne(new LambdaQueryWrapper<ApprovalFlow>()
                .eq(ApprovalFlow::getFlowCode, flowCode)
                .eq(ApprovalFlow::getStatus, 1)
                .orderByDesc(ApprovalFlow::getVersion)
                .last("LIMIT 1"));
    }

    private FlowDefinition.Node nextNode(FlowDefinition definition, int afterSeq) {
        if (definition == null || definition.nodes() == null) {
            return null;
        }
        return definition.nodes().stream()
                .filter(node -> node.seq() != null && node.seq() > afterSeq)
                .filter(node -> !node.isOptional())
                .min(Comparator.comparing(FlowDefinition.Node::seq))
                .orElse(null);
    }

    private void createTask(ApprovalInstance instance, FlowDefinition definition, FlowDefinition.Node node) {
        int timeoutHours = definition.timeoutHours() == null ? 24 : definition.timeoutHours();
        ApprovalTask task = new ApprovalTask();
        task.setInstanceId(instance.getId());
        task.setSeq(node.seq());
        task.setNodeCode(node.assignee());
        task.setNodeName(node.name());
        task.setAssigneeType(AssigneeType.of(node.assignee()).name());
        Long assigneeId = "HEAD_TEACHER".equalsIgnoreCase(node.assignee()) && instance.getAssigneeHint() != null
                ? instance.getAssigneeHint()
                : assigneeResolver.resolveUserId(node.assignee(), instance.getTargetId());
        task.setAssigneeId(assigneeId);
        task.setStatus(ApprovalStatuses.PENDING);
        task.setEmergency(0);
        task.setDueAt(LocalDateTime.now().plusHours(timeoutHours));
        task.setCreatedAt(LocalDateTime.now());
        task.setUpdatedAt(LocalDateTime.now());
        taskMapper.insert(task);

        // 第一级被访人由预约提交时的“新的访客申请”通知触达，其余审批节点统一发待办通知
        if (assigneeId != null && !"TARGET".equals(task.getAssigneeType())) {
            notificationService.notifyUser(assigneeId, "新的审批待办",
                    node.name() + "，请及时处理", "APPROVAL", "APPROVAL_TASK", task.getId());
        }

        instance.setCurrentSeq(node.seq());
        instance.setUpdatedAt(LocalDateTime.now());
        instanceMapper.updateById(instance);
    }

    private void complete(ApprovalInstance instance, String status) {
        instance.setStatus(status);
        instance.setEndedAt(LocalDateTime.now());
        instance.setUpdatedAt(LocalDateTime.now());
        instanceMapper.updateById(instance);
        eventPublisher.publishEvent(new ApprovalCompletedEvent(instance.getId(), instance.getBizType(),
                instance.getBizId(), instance.getSchoolId(), status));
    }

    private void skipPending(Long instanceId) {
        List<ApprovalTask> pending = taskMapper.selectList(new LambdaQueryWrapper<ApprovalTask>()
                .eq(ApprovalTask::getInstanceId, instanceId)
                .eq(ApprovalTask::getStatus, ApprovalStatuses.PENDING));
        for (ApprovalTask task : pending) {
            task.setStatus(ApprovalStatuses.SKIPPED);
            task.setUpdatedAt(LocalDateTime.now());
            taskMapper.updateById(task);
        }
    }

    private ApprovalTask pendingTask(Long taskId) {
        ApprovalTask task = taskMapper.selectById(taskId);
        if (task == null) {
            throw new BizException(404, "审批任务不存在");
        }
        if (!ApprovalStatuses.PENDING.equals(task.getStatus())) {
            throw new BizException(400, "任务已处理，请勿重复操作");
        }
        return task;
    }

    private ApprovalInstance runningInstance(Long instanceId) {
        ApprovalInstance instance = instanceMapper.selectById(instanceId);
        if (instance == null) {
            throw new BizException(404, "审批实例不存在");
        }
        if (!ApprovalStatuses.RUNNING.equals(instance.getStatus())) {
            throw new BizException(400, "审批已结束");
        }
        return instance;
    }

    private void notifyProxyHandled(ApprovalTask task, boolean approved, String comment) {
        Long assigneeId = task.getAssigneeId();
        Long actorId = UserContext.getUserId();
        if (assigneeId == null || assigneeId.equals(actorId)) {
            return;
        }
        String action = approved ? "代通过" : "代驳回";
        String actor = UserContext.getName() == null ? "管理员" : UserContext.getName();
        String detail = "您的「" + task.getNodeName() + "」审批任务已由" + actor + action;
        if (comment != null && !comment.isBlank()) {
            detail += "，意见：" + comment;
        }
        notificationService.notifyUser(assigneeId, "审批任务已" + action,
                detail, "APPROVAL", "APPROVAL_TASK", task.getId());
    }
    private void authorize(ApprovalTask task, ApprovalInstance instance, boolean emergency) {
        RoleEnum role = UserContext.getRole();
        Long me = UserContext.getUserId();
        boolean actualAdmin = roleAccessChecker.hasAny(RoleEnum.SECURITY_ADMIN, RoleEnum.SYSTEM_ADMIN);

        if (emergency) {
            if (!actualAdmin) {
                throw new BizException(403, "仅管理员可紧急审批");
            }
            checkSchoolScope(instance.getSchoolId());
            return;
        }
        if (task.getAssigneeId() != null) {
            if (task.getAssigneeId().equals(me)) {
                return;
            }
            if (actualAdmin && "SECURITY".equals(task.getAssigneeType())) {
                checkSchoolScope(instance.getSchoolId());
                return;
            }
            throw new BizException(403, "不是当前审批人");
        }
        if (!actualAdmin) {
            throw new BizException(403, "该节点由管理员处理");
        }
        checkSchoolScope(instance.getSchoolId());
    }

    private void checkSchoolScope(Long schoolId) {
        RoleEnum role = UserContext.getRole();
        if (role == RoleEnum.SYSTEM_ADMIN) {
            return;
        }
        if (!Objects.equals(UserContext.getSchoolId(), schoolId)) {
            throw new BizException(403, "无权操作其他学校的审批");
        }
    }

    private FlowDefinition read(String json) {
        try {
            return objectMapper.readValue(json, FlowDefinition.class);
        } catch (Exception e) {
            throw new BizException(400, "流程定义解析失败");
        }
    }
}
