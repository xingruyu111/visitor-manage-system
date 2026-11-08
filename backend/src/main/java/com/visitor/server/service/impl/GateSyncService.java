package com.visitor.server.service.impl;

import com.visitor.domain.entity.VisitorPass;
import com.visitor.domain.entity.VisitorRecord;
import com.visitor.integration.IntegrationFactory;
import com.visitor.integration.gate.GateClient;
import com.visitor.domain.entity.UserRole;
import com.visitor.common.enums.RoleEnum;
import com.visitor.server.mapper.UserRoleMapper;
import com.visitor.server.service.NotificationService;
import com.visitor.server.service.AuditLogService;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * 门闸/停车场下发：审批通过下发权限、取消/过期撤销、黑名单车辆同步。
 * 未配置时全部静默降级，不影响主流程。
 */
@Slf4j
@Component
public class GateSyncService {

    private final IntegrationFactory integrationFactory;
    private final NotificationService notificationService;
    private final UserRoleMapper userRoleMapper;
    private final AuditLogService auditLogService;

    public GateSyncService(IntegrationFactory integrationFactory, NotificationService notificationService,
                           UserRoleMapper userRoleMapper, AuditLogService auditLogService) {
        this.integrationFactory = integrationFactory;
        this.notificationService = notificationService;
        this.userRoleMapper = userRoleMapper;
        this.auditLogService = auditLogService;
    }

    public void push(VisitorRecord record, VisitorPass pass) {
        if (record.getPlateNumber() == null || record.getPlateNumber().isBlank()) {
            return;
        }
        GateClient.GatePermission permission = new GateClient.GatePermission(
                pass.getPassNo(), record.getPlateNumber(), "CAR",
                record.getVisitorName(), record.getVisitorPhone(),
                pass.getValidFrom(), pass.getValidTo());
        for (int attempt = 1; attempt <= 3; attempt++) {
            try {
                GateClient gate = integrationFactory.gate();
                if (gate == null || !gate.isConfigured()) {
                    return;
                }
                GateClient.GateOpResult result = gate.pushPermission(permission);
                if (result != null && result.success()) {
                    return;
                }
                log.warn("门闸权限下发失败（第 {} 次）: passNo={}, msg={}", attempt, pass.getPassNo(),
                        result == null ? "null" : result.message());
            } catch (Exception e) {
                log.warn("门闸权限下发异常（第 {} 次）: passNo={}, err={}", attempt, pass.getPassNo(), e.getMessage());
            }
        }
        notifyFailure("门闸权限下发失败", "passNo=" + pass.getPassNo() + ", plate=" + record.getPlateNumber());
    }

    private void notifyFailure(String title, String detail) {
        try {
            auditLogService.record("GATE_PUSH_FAILED", "VISITOR_PASS", null, title + "：" + detail);
        } catch (Exception ignored) {
            // 审计失败不影响告警
        }
        try {
            for (UserRole role : userRoleMapper.selectList(new LambdaQueryWrapper<UserRole>()
                    .eq(UserRole::getRole, RoleEnum.SECURITY_ADMIN)
                    .eq(UserRole::getStatus, 1))) {
                notificationService.notifyUser(role.getUserId(), title, detail, "ACCESS_ALERT", null, null);
            }
        } catch (Exception e) {
            log.warn("门闸失败告警发送异常（已忽略）: {}", e.getMessage());
        }
    }

    public void revoke(String passNo) {
        try {
            GateClient gate = integrationFactory.gate();
            if (gate != null && gate.isConfigured()) {
                gate.revokePermission(passNo);
            }
        } catch (Exception e) {
            log.warn("门闸权限撤销异常（已忽略）: passNo={}, err={}", passNo, e.getMessage());
        }
    }

    public void syncBlacklist(String plateNo) {
        if (plateNo == null || plateNo.isBlank()) {
            return;
        }
        try {
            GateClient gate = integrationFactory.gate();
            if (gate != null && gate.isConfigured()) {
                gate.syncBlacklist(List.of(plateNo.toUpperCase()));
            }
        } catch (Exception e) {
            log.warn("门闸黑名单同步异常（已忽略）: plate={}, err={}", plateNo, e.getMessage());
        }
    }
}
