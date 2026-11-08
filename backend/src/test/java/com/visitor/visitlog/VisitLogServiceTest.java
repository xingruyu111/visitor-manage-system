package com.visitor.visitlog;

import com.visitor.domain.entity.VisitLog;
import com.visitor.domain.entity.VisitorRecord;
import com.visitor.domain.vo.VisitLogVO;
import com.visitor.server.mapper.VisitLogMapper;
import com.visitor.server.mapper.VisitorRecordMapper;
import com.visitor.server.service.VisitLogService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@ActiveProfiles("test")
class VisitLogServiceTest {

    @Autowired
    private VisitLogService service;

    @Autowired
    private VisitLogMapper visitLogMapper;

    @Autowired
    private VisitorRecordMapper recordMapper;

    @BeforeEach
    void setUp() {
        visitLogMapper.delete(null);
        recordMapper.delete(null);
    }

    @Test
    void pageAndExportIncludeVisitorInfo() {
        Long recordId = record();
        insertLog(recordId, "IN", "LOCAL");
        insertLog(recordId, "OUT", "GATE");

        assertThat(service.page(null, null, null, null, 1, 10).total()).isEqualTo(2);
        List<VisitLogVO> inbound = service.page("IN", null, null, null, 1, 10).records();
        assertThat(inbound).hasSize(1);
        assertThat(inbound.get(0).getVisitorName()).isEqualTo("通行访客");
        assertThat(inbound.get(0).getPlateNumber()).isEqualTo("粤B12345");
        assertThat(service.page(null, "GATE", null, null, 1, 10).total()).isEqualTo(1);
        assertThat(service.exportRows(null, null, null, null)).hasSize(2);
    }

    @Test
    void timeRangeFiltersLogs() {
        Long recordId = record();
        VisitLog old = insertLog(recordId, "IN", "LOCAL");
        old.setEventTime(LocalDateTime.now().minusDays(3));
        visitLogMapper.updateById(old);
        insertLog(recordId, "OUT", "LOCAL");

        LocalDateTime from = LocalDateTime.now().minusDays(1);
        assertThat(service.page(null, null, from, null, 1, 10).total()).isEqualTo(1);
    }

    @Test
    void emptyResultIsHandled() {
        assertThat(service.page(null, null, null, null, 1, 10).total()).isZero();
        assertThat(service.exportRows(null, null, null, null)).isEmpty();
    }
    private Long record() {
        VisitorRecord record = new VisitorRecord();
        record.setSchoolId(1L);
        record.setVisitorUserId(1L);
        record.setVisitorName("通行访客");
        record.setVisitorType("NORMAL");
        record.setPlateNumber("粤B12345");
        record.setExpectedTime(LocalDateTime.now().minusHours(1));
        record.setLeaveTime(LocalDateTime.now().plusHours(2));
        record.setStatus(3);
        record.setCreatedAt(LocalDateTime.now());
        record.setUpdatedAt(LocalDateTime.now());
        recordMapper.insert(record);
        return record.getId();
    }

    private VisitLog insertLog(Long recordId, String direction, String source) {
        VisitLog log = new VisitLog();
        log.setSchoolId(1L);
        log.setRecordId(recordId);
        log.setDirection(direction);
        log.setSource(source);
        log.setEventTime(LocalDateTime.now());
        log.setCreatedAt(LocalDateTime.now());
        visitLogMapper.insert(log);
        return log;
    }
}
