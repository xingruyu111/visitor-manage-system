package com.visitor.server.service.impl;

import com.visitor.domain.entity.VisitorPass;
import com.visitor.domain.entity.VisitorRecord;
import com.visitor.integration.IntegrationFactory;
import com.visitor.integration.gate.GateClient;
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

    public GateSyncService(IntegrationFactory integrationFactory) {
        this.integrationFactory = integrationFactory;
    }

    public void push(VisitorRecord record, VisitorPass pass) {
        if (record.getPlateNumber() == null || record.getPlateNumber().isBlank()) {
            return;
        }
        try {
            GateClient gate = integrationFactory.gate();
            if (gate == null || !gate.isConfigured()) {
                return;
            }
            GateClient.GateOpResult result = gate.pushPermission(new GateClient.GatePermission(
                    pass.getPassNo(), record.getPlateNumber(), "CAR",
                    record.getVisitorName(), record.getVisitorPhone(),
                    pass.getValidFrom(), pass.getValidTo()));
            if (result == null || !result.success()) {
                log.warn("门闸权限下发失败: passNo={}, msg={}", pass.getPassNo(),
                        result == null ? "null" : result.message());
            }
        } catch (Exception e) {
            log.warn("门闸权限下发异常（已忽略）: passNo={}, err={}", pass.getPassNo(), e.getMessage());
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
