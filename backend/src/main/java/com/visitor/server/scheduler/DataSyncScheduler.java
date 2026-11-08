package com.visitor.server.scheduler;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.visitor.common.enums.RoleEnum;
import com.visitor.domain.entity.UserRole;
import com.visitor.server.mapper.UserRoleMapper;
import com.visitor.server.service.AuditLogService;
import com.visitor.server.service.NotificationService;
import com.visitor.server.service.OrgSyncService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * 数据中台定时同步：每日 2 点同步组织/教职工/学生/校友，失败重试 3 次并告警。
 */
@Slf4j
@Component
public class DataSyncScheduler {

    private static final List<String> TYPES = List.of("org", "staff", "student", "alumni");

    private final OrgSyncService orgSyncService;
    private final NotificationService notificationService;
    private final UserRoleMapper userRoleMapper;
    private final AuditLogService auditLogService;

    public DataSyncScheduler(OrgSyncService orgSyncService, NotificationService notificationService,
                             UserRoleMapper userRoleMapper, AuditLogService auditLogService) {
        this.orgSyncService = orgSyncService;
        this.notificationService = notificationService;
        this.userRoleMapper = userRoleMapper;
        this.auditLogService = auditLogService;
    }

    @Scheduled(cron = "0 0 2 * * ?")
    public void dailySync() {
        try {
            int failed = syncAll();
            if (failed > 0) {
                log.warn("数据中台定时同步完成，失败 {} 类", failed);
            }
        } catch (Exception e) {
            log.error("数据中台定时同步异常", e);
        }
    }

    /** 返回最终失败的同步类型数量。 */
    public int syncAll() {
        int failed = 0;
        for (String type : TYPES) {
            if (!syncWithRetry(type)) {
                failed++;
            }
        }
        return failed;
    }

    private boolean syncWithRetry(String type) {
        for (int attempt = 1; attempt <= 3; attempt++) {
            try {
                OrgSyncService.SyncResult result = orgSyncService.sync(1L, type);
                if (result != null && result.failed() == 0) {
                    return true;
                }
                log.warn("同步 {} 存在失败记录（第 {} 次）: failed={}", type, attempt,
                        result == null ? "null" : result.failed());
            } catch (Exception e) {
                log.warn("同步 {} 异常（第 {} 次）: {}", type, attempt, e.getMessage());
            }
        }
        notifyFailure(type);
        return false;
    }

    private void notifyFailure(String type) {
        try {
            auditLogService.record("DATA_SYNC_FAILED", "ORG_SYNC", null, "数据中台同步失败：" + type);
        } catch (Exception ignored) {
            // 审计失败不影响告警
        }
        try {
            for (UserRole role : userRoleMapper.selectList(new LambdaQueryWrapper<UserRole>()
                    .eq(UserRole::getRole, RoleEnum.SECURITY_ADMIN)
                    .eq(UserRole::getStatus, 1))) {
                notificationService.notifyUser(role.getUserId(), "数据中台同步失败",
                        "同步类型：" + type + "，已重试 3 次", "SYSTEM", "ORG_SYNC", null);
            }
        } catch (Exception e) {
            log.warn("同步失败告警发送异常（已忽略）: {}", e.getMessage());
        }
    }
}
