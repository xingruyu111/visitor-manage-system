package com.visitor.server.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.visitor.domain.entity.VisitLog;
import com.visitor.domain.entity.VisitorPass;
import com.visitor.domain.entity.VisitorRecord;
import com.visitor.integration.IntegrationFactory;
import com.visitor.integration.gate.GateClient;
import com.visitor.server.mapper.VisitLogMapper;
import com.visitor.server.mapper.VisitorPassMapper;
import com.visitor.server.mapper.VisitorRecordMapper;
import com.visitor.server.service.GateRecordIntakeService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 门闸通行记录回传：拉取记录 → 去重 → 更新访客状态 → 写通行流水。
 */
@Slf4j
@Service
public class GateRecordIntakeServiceImpl implements GateRecordIntakeService {

    private final IntegrationFactory integrationFactory;
    private final VisitorRecordMapper recordMapper;
    private final VisitorPassMapper passMapper;
    private final VisitLogMapper visitLogMapper;

    public GateRecordIntakeServiceImpl(IntegrationFactory integrationFactory, VisitorRecordMapper recordMapper,
                                       VisitorPassMapper passMapper, VisitLogMapper visitLogMapper) {
        this.integrationFactory = integrationFactory;
        this.recordMapper = recordMapper;
        this.passMapper = passMapper;
        this.visitLogMapper = visitLogMapper;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public int pull() {
        GateClient gate = integrationFactory.gate();
        if (gate == null || !gate.isConfigured()) {
            return 0;
        }
        LocalDateTime now = LocalDateTime.now();
        List<GateClient.GateRecord> records = gate.pullRecords(now.minusHours(2), now);
        int applied = 0;
        for (GateClient.GateRecord gateRecord : records) {
            Long exists = visitLogMapper.selectCount(new LambdaQueryWrapper<VisitLog>()
                    .eq(VisitLog::getExternalId, gateRecord.externalId()));
            if (exists != null && exists > 0) {
                continue;
            }
            VisitorRecord record = findRecord(gateRecord.plateNo());
            if (record == null) {
                log.warn("门闸记录未匹配到预约: plate={}", gateRecord.plateNo());
                continue;
            }
            boolean inbound = "IN".equalsIgnoreCase(gateRecord.direction());
            if (inbound && record.getStatus() != 1 && record.getStatus() != 4) {
                continue;
            }
            if (!inbound && record.getStatus() != 3) {
                continue;
            }
            record.setStatus(inbound ? 3 : 4);
            record.setUpdatedAt(now);
            recordMapper.updateById(record);

            VisitLog visitLog = new VisitLog();
            visitLog.setSchoolId(record.getSchoolId());
            visitLog.setRecordId(record.getId());
            visitLog.setPassId(findPassId(record.getId()));
            visitLog.setDirection(inbound ? "IN" : "OUT");
            visitLog.setSource("GATE");
            visitLog.setExternalId(gateRecord.externalId());
            visitLog.setEventTime(gateRecord.eventTime());
            visitLog.setCreatedAt(now);
            visitLogMapper.insert(visitLog);
            applied++;
        }
        return applied;
    }

    private VisitorRecord findRecord(String plateNo) {
        if (plateNo == null || plateNo.isBlank()) {
            return null;
        }
        List<VisitorRecord> list = recordMapper.selectList(new LambdaQueryWrapper<VisitorRecord>()
                .eq(VisitorRecord::getPlateNumber, plateNo.trim().toUpperCase())
                .orderByDesc(VisitorRecord::getId)
                .last("LIMIT 1"));
        return list.isEmpty() ? null : list.get(0);
    }

    private Long findPassId(Long recordId) {
        VisitorPass pass = passMapper.selectOne(new LambdaQueryWrapper<VisitorPass>()
                .eq(VisitorPass::getRecordId, recordId).last("LIMIT 1"));
        return pass == null ? null : pass.getId();
    }
}
