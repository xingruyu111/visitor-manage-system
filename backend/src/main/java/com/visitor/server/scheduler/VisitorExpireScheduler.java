package com.visitor.server.scheduler;

import com.visitor.server.service.VisitorService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * 每 5 分钟把离校时间已过的待审批/已通过/已拒绝记录标记为过期。
 */
@Component
public class VisitorExpireScheduler {

    private static final Logger log = LoggerFactory.getLogger(VisitorExpireScheduler.class);

    private final VisitorService visitorService;

    public VisitorExpireScheduler(VisitorService visitorService) {
        this.visitorService = visitorService;
    }

    @Scheduled(cron = "0 */5 * * * ?")
    public void expireOverdueRecords() {
        try {
            int expired = visitorService.expireOverdue();
            if (expired > 0) {
                log.info("访客记录过期: count={}", expired);
            }
        } catch (Exception e) {
            log.error("访客记录过期扫描失败", e);
        }
    }
}
