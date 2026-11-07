package com.visitor.server.service.impl;

import com.visitor.domain.entity.VisitorRecord;
import com.visitor.server.mapper.VisitorRecordMapper;
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

    public VisitorApprovalListener(VisitorRecordMapper recordMapper, VisitorPassIssuer passIssuer) {
        this.recordMapper = recordMapper;
        this.passIssuer = passIssuer;
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
            log.info("访客预约通过并签发凭证: recordId={}", record.getId());
        } else if (ApprovalStatuses.REJECTED.equals(event.status())) {
            record.setStatus(2);
            record.setUpdatedAt(LocalDateTime.now());
            recordMapper.updateById(record);
            log.info("访客预约被驳回: recordId={}", record.getId());
        }
    }
}
