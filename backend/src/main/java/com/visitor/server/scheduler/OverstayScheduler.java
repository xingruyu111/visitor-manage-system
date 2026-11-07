package com.visitor.server.scheduler;

import com.visitor.server.service.AlarmService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/** 每 10 分钟检查一次超期滞留。 */
@Component
public class OverstayScheduler {

    private static final Logger log = LoggerFactory.getLogger(OverstayScheduler.class);

    private final AlarmService alarmService;

    public OverstayScheduler(AlarmService alarmService) {
        this.alarmService = alarmService;
    }

    @Scheduled(cron = "0 */10 * * * ?")
    public void check() {
        try {
            int created = alarmService.checkOverstay();
            if (created > 0) {
                log.info("超期滞留告警生成: count={}", created);
            }
        } catch (Exception e) {
            log.error("超期滞留检查失败", e);
        }
    }
}
