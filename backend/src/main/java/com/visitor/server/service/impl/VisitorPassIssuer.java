package com.visitor.server.service.impl;

import com.visitor.domain.entity.VisitorPass;
import com.visitor.domain.entity.VisitorRecord;
import com.visitor.server.mapper.VisitorPassMapper;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * 通行凭证签发：预约通过时生成，同一记录只签发一次。
 */
@Component
public class VisitorPassIssuer {

    private final VisitorPassMapper passMapper;
    private final GateSyncService gateSyncService;

    public VisitorPassIssuer(VisitorPassMapper passMapper, GateSyncService gateSyncService) {
        this.passMapper = passMapper;
        this.gateSyncService = gateSyncService;
    }

    public VisitorPass issue(VisitorRecord record) {
        VisitorPass existing = passMapper.selectOne(
                new com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<VisitorPass>()
                        .eq(VisitorPass::getRecordId, record.getId()));
        if (existing != null) {
            return existing;
        }
        VisitorPass pass = new VisitorPass();
        pass.setSchoolId(record.getSchoolId());
        pass.setRecordId(record.getId());
        pass.setPassNo("P" + record.getId() + "-" + System.currentTimeMillis());
        pass.setQrToken(UUID.randomUUID().toString());
        pass.setValidFrom(record.getExpectedTime());
        LocalDateTime validTo = record.getLeaveTime();
        if ("ALUMNI".equals(record.getVisitorType())) {
            LocalDateTime monthEnd = record.getExpectedTime().plusDays(30);
            if (monthEnd.isAfter(validTo)) {
                validTo = monthEnd;
            }
        }
        pass.setValidTo(validTo);
        if ("WHITELIST".equals(record.getTargetType())) {
            // 白名单在有效期内不限次进出
            pass.setMaxEntries(999999);
        } else {
            pass.setMaxEntries("ALUMNI".equals(record.getVisitorType()) ? 30 : 1);
        }
        pass.setUsedEntries(0);
        pass.setStatus(0);
        pass.setCreatedAt(LocalDateTime.now());
        pass.setUpdatedAt(LocalDateTime.now());
        passMapper.insert(pass);
        gateSyncService.push(record, pass);
        return pass;
    }

    /** 过期：凭证置为过期并通知门闸撤销。 */
    public void expire(Long recordId) {
        VisitorPass pass = passMapper.selectOne(
                new com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<VisitorPass>()
                        .eq(VisitorPass::getRecordId, recordId));
        if (pass != null && pass.getStatus() == 0) {
            pass.setStatus(2);
            pass.setUpdatedAt(LocalDateTime.now());
            passMapper.updateById(pass);
            gateSyncService.revoke(pass.getPassNo());
        }
    }

    public void revoke(Long recordId) {
        VisitorPass pass = passMapper.selectOne(
                new com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<VisitorPass>()
                        .eq(VisitorPass::getRecordId, recordId));
        if (pass != null && pass.getStatus() == 0) {
            pass.setStatus(3);
            pass.setUpdatedAt(LocalDateTime.now());
            passMapper.updateById(pass);
            gateSyncService.revoke(pass.getPassNo());
        }
    }
}
