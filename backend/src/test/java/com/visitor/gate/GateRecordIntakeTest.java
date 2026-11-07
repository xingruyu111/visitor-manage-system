package com.visitor.gate;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.visitor.domain.entity.IntegrationConfig;
import com.visitor.domain.entity.VisitLog;
import com.visitor.domain.entity.VisitorRecord;
import com.visitor.integration.gate.GateClient;
import com.visitor.integration.gate.MockGateClient;
import com.visitor.server.mapper.IntegrationConfigMapper;
import com.visitor.server.mapper.VisitLogMapper;
import com.visitor.server.mapper.VisitorRecordMapper;
import com.visitor.server.service.GateRecordIntakeService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@ActiveProfiles("test")
class GateRecordIntakeTest {

    @Autowired
    private GateRecordIntakeService intakeService;

    @Autowired
    private MockGateClient mockGateClient;

    @Autowired
    private IntegrationConfigMapper configMapper;

    @Autowired
    private VisitorRecordMapper recordMapper;

    @Autowired
    private VisitLogMapper visitLogMapper;

    @BeforeEach
    void setUp() {
        visitLogMapper.delete(null);
        recordMapper.delete(null);
        mockGateClient.clear();
        IntegrationConfig config = configMapper.selectOne(new LambdaQueryWrapper<IntegrationConfig>()
                .eq(IntegrationConfig::getIntegrationKey, "GATE"));
        if (config == null) {
            config = new IntegrationConfig();
            config.setIntegrationKey("GATE");
            config.setEnabled(1);
            config.setProtocol("MOCK");
            config.setStatus(1);
            config.setCreatedAt(LocalDateTime.now());
            config.setUpdatedAt(LocalDateTime.now());
            configMapper.insert(config);
        } else {
            config.setEnabled(1);
            config.setProtocol("MOCK");
            configMapper.updateById(config);
        }
    }

    @Test
    void inboundRecordIsAppliedAndDeduplicated() {
        VisitorRecord record = record("粤B99999", 1);
        mockGateClient.offer(new GateClient.GateRecord("ext-1", "粤B99999", "IN", LocalDateTime.now(), "gate-1"));

        assertThat(intakeService.pull()).isEqualTo(1);
        assertThat(recordMapper.selectById(record.getId()).getStatus()).isEqualTo(3);
        VisitLog log = visitLogMapper.selectList(null).get(0);
        assertThat(log.getSource()).isEqualTo("GATE");
        assertThat(log.getExternalId()).isEqualTo("ext-1");

        assertThat(intakeService.pull()).isZero();
    }

    @Test
    void outboundRequiresInSchoolStatus() {
        record("粤B88888", 1);
        mockGateClient.offer(new GateClient.GateRecord("ext-2", "粤B88888", "OUT", LocalDateTime.now(), "gate-1"));

        assertThat(intakeService.pull()).isZero();
        assertThat(visitLogMapper.selectCount(null)).isZero();
    }

    @Test
    void unmatchedPlateIsIgnored() {
        mockGateClient.offer(new GateClient.GateRecord("ext-3", "粤B00000", "IN", LocalDateTime.now(), "gate-1"));

        assertThat(intakeService.pull()).isZero();
        assertThat(visitLogMapper.selectCount(null)).isZero();
    }
    private VisitorRecord record(String plate, int status) {
        VisitorRecord record = new VisitorRecord();
        record.setSchoolId(1L);
        record.setVisitorUserId(1L);
        record.setVisitorName("门闸访客");
        record.setVisitorType("NORMAL");
        record.setPlateNumber(plate);
        record.setExpectedTime(LocalDateTime.now().minusHours(1));
        record.setLeaveTime(LocalDateTime.now().plusHours(2));
        record.setStatus(status);
        record.setCreatedAt(LocalDateTime.now());
        record.setUpdatedAt(LocalDateTime.now());
        recordMapper.insert(record);
        return record;
    }
}
