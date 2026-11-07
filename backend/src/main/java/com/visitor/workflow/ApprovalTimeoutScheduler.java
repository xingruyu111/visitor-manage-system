package com.visitor.workflow;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * 每 5 分钟扫描超时任务：置为 EXPIRED，并自动驳回所属实例（24 小时未处理自动驳回）。
 */
@Component
public class ApprovalTimeoutScheduler {

    private static final Logger log = LoggerFactory.getLogger(ApprovalTimeoutScheduler.class);

    private final ApprovalEngine approvalEngine;

    public ApprovalTimeoutScheduler(ApprovalEngine approvalEngine) {
        this.approvalEngine = approvalEngine;
    }

    @Scheduled(cron = "0 */5 * * * ?")
    public void expireOverdueTasks() {
        try {
            int expired = approvalEngine.expireOverdueTasks();
            if (expired > 0) {
                log.info("审批超时自动驳回: count={}", expired);
            }
        } catch (Exception e) {
            log.error("审批超时扫描失败", e);
        }
    }
}
