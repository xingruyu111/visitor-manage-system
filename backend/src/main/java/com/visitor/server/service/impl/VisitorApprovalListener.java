package com.visitor.server.service.impl;

import com.visitor.domain.entity.VisitorRecord;
import com.visitor.server.mapper.VisitorRecordMapper;
import com.visitor.server.service.NotificationService;
import com.visitor.workflow.ApprovalCompletedEvent;
import com.visitor.workflow.ApprovalStatuses;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

/**
 * 审批结束回调：通过则更新记录状态并签发凭证，驳回则标记拒绝。
 */
@Component
public class VisitorApprovalListener {

    private static final Logger log = LoggerFactory.getLogger(VisitorApprovalListener.class);

    private final VisitorRecordMapper recordMapper;
    private final VisitorPassIssuer passIssuer;
    private final NotificationService notificationService;

    public VisitorApprovalListener(VisitorRecordMapper recordMapper, VisitorPassIssuer passIssuer, NotificationService notificationService) {
        this.recordMapper = recordMapper;
        this.passIssuer = passIssuer;
        this.notificationService = notificationService;
    }

    @EventListener
    @Transactional(rollbackFor = Exception.class)
    public void onApprovalCompleted(ApprovalCompletedEvent event) {
        if (!"VISITOR_RECORD".equals(event.bizType())) {
            return;
        }
        VisitorRecord record = recordMapper.selectById(event.bizId());
        if (record == null) {
            return;
        }
        if (ApprovalStatuses.APPROVED.equals(event.status())) {
            record.setStatus(1);
            record.setUpdatedAt(LocalDateTime.now());
            recordMapper.updateById(record);
            passIssuer.issue(record);
            notificationService.notifyUser(record.getVisitorUserId(), "预约已通过",
                    "您的访客预约已通过，请在「我的预约」中查看通行码", "APPROVAL", "VISITOR_RECORD", record.getId());
            log.info("访客预约通过并签发凭证: recordId={}", record.getId());
        } else if (ApprovalStatuses.REJECTED.equals(event.status())) {
            record.setStatus(2);
            record.setUpdatedAt(LocalDateTime.now());
            recordMapper.updateById(record);
            notificationService.notifyUser(record.getVisitorUserId(), "预约被驳回",
                    "您的访客预约未通过，请查看详情或联系被访人", "APPROVAL", "VISITOR_RECORD", record.getId());
            log.info("访客预约被驳回: recordId={}", record.getId());
        }
    }
}
