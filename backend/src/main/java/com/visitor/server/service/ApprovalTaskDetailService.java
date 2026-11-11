package com.visitor.server.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.visitor.common.enums.RoleEnum;
import com.visitor.common.exception.BizException;
import com.visitor.domain.entity.ApprovalInstance;
import com.visitor.domain.entity.ApprovalTask;
import com.visitor.domain.entity.VisitorRecord;
import com.visitor.security.UserContext;
import com.visitor.server.mapper.ApprovalInstanceMapper;
import com.visitor.server.mapper.ApprovalTaskMapper;
import com.visitor.server.mapper.VisitorRecordMapper;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Service
public class ApprovalTaskDetailService {

    private final ApprovalTaskMapper taskMapper;
    private final ApprovalInstanceMapper instanceMapper;
    private final VisitorRecordMapper recordMapper;

    public ApprovalTaskDetailService(ApprovalTaskMapper taskMapper,
                                     ApprovalInstanceMapper instanceMapper,
                                     VisitorRecordMapper recordMapper) {
        this.taskMapper = taskMapper;
        this.instanceMapper = instanceMapper;
        this.recordMapper = recordMapper;
    }

    public Map<String, Object> detail(Long taskId) {
        ApprovalTask task = taskMapper.selectById(taskId);
        if (task == null) {
            throw new BizException(404, "审批任务不存在");
        }
        Long userId = UserContext.getUserId();
        RoleEnum role = UserContext.getRole();
        boolean admin = role == RoleEnum.SYSTEM_ADMIN || role == RoleEnum.SECURITY_ADMIN;
        if (!admin && !userId.equals(task.getAssigneeId())) {
            throw new BizException(403, "无权查看该审批任务");
        }

        ApprovalInstance instance = instanceMapper.selectById(task.getInstanceId());
        VisitorRecord record = null;
        if (instance != null && "VISITOR_RECORD".equals(instance.getBizType())) {
            record = recordMapper.selectById(instance.getBizId());
        }

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("task", task);
        result.put("record", record);
        result.put("history", history(task.getInstanceId()));
        return result;
    }

    private List<Map<String, Object>> history(Long instanceId) {
        List<Map<String, Object>> history = new ArrayList<>();
        if (instanceId == null) {
            return history;
        }
        for (ApprovalTask item : taskMapper.selectList(new LambdaQueryWrapper<ApprovalTask>()
                .eq(ApprovalTask::getInstanceId, instanceId)
                .orderByAsc(ApprovalTask::getSeq))) {
            Map<String, Object> node = new LinkedHashMap<>();
            node.put("taskId", item.getId());
            node.put("nodeName", item.getNodeName());
            node.put("status", item.getStatus());
            node.put("decision", item.getDecision());
            node.put("comment", item.getComment());
            node.put("actedAt", item.getActedAt());
            history.add(node);
        }
        return history;
    }
}
