package com.visitor.server.scheduler;

import com.visitor.server.service.AccessCheckService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * 每 10 分钟把过了失效时间的名单置为失效（黑名单到期自动解除）。
 */
@Component
public class AccessListExpireScheduler {

    private static final Logger log = LoggerFactory.getLogger(AccessListExpireScheduler.class);

    private final AccessCheckService accessCheckService;

    public AccessListExpireScheduler(AccessCheckService accessCheckService) {
        this.accessCheckService = accessCheckService;
    }

    @Scheduled(cron = "0 */10 * * * ?")
    public void expireOverdue() {
        try {
            int expired = accessCheckService.expireOverdue();
            if (expired > 0) {
                log.info("名单到期失效: count={}", expired);
            }
        } catch (Exception e) {
            log.error("名单到期扫描失败", e);
        }
    }
}
