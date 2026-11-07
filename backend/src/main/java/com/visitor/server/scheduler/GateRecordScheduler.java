package com.visitor.server.scheduler;

import com.visitor.server.service.GateRecordIntakeService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/** 每 5 分钟拉取门闸通行记录。 */
@Component
public class GateRecordScheduler {

    private static final Logger log = LoggerFactory.getLogger(GateRecordScheduler.class);

    private final GateRecordIntakeService intakeService;

    public GateRecordScheduler(GateRecordIntakeService intakeService) {
        this.intakeService = intakeService;
    }

    @Scheduled(cron = "0 */5 * * * ?")
    public void pull() {
        try {
            int applied = intakeService.pull();
            if (applied > 0) {
                log.info("门闸通行记录回传: count={}", applied);
            }
        } catch (Exception e) {
            log.error("门闸记录回传失败", e);
        }
    }
}
