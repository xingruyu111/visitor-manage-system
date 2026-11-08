package com.visitor.workflow;

import com.visitor.common.core.PageResult;
import com.visitor.domain.entity.ApprovalInstance;
import com.visitor.domain.entity.ApprovalTask;

import java.util.List;

public interface ApprovalEngine {

    ApprovalInstance start(StartCommand command);

    ApprovalTask approve(Long taskId, String comment, boolean emergency);

    ApprovalTask reject(Long taskId, String comment, boolean emergency);

    List<ApprovalTask> batchApprove(List<Long> taskIds, String comment);

    List<ApprovalTask> batchReject(List<Long> taskIds, String comment);

    void cancel(Long instanceId, String reason);

    int expireOverdueTasks();

    PageResult<ApprovalTask> myTasks(int page, int size);

    PageResult<ApprovalInstance> instances(Long schoolId, String status, int page, int size);

    record StartCommand(String bizType, Long bizId, Long schoolId, Long applicantId,
                        String visitorType, String targetType, Long targetId, String flowCode, Long assigneeHint) {

        public StartCommand(String bizType, Long bizId, Long schoolId, Long applicantId,
                            String visitorType, String targetType, Long targetId, String flowCode) {
            this(bizType, bizId, schoolId, applicantId, visitorType, targetType, targetId, flowCode, null);
        }

        public StartCommand(String bizType, Long bizId, Long schoolId, Long applicantId,
                            String visitorType, String targetType, Long targetId) {
            this(bizType, bizId, schoolId, applicantId, visitorType, targetType, targetId, null, null);
        }
    }
}
