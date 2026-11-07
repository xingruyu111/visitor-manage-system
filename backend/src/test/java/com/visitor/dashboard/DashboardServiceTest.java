package com.visitor.dashboard;

import com.visitor.domain.entity.ApprovalTask;
import com.visitor.domain.entity.VisitorRecord;
import com.visitor.server.mapper.ApprovalTaskMapper;
import com.visitor.server.mapper.VisitorRecordMapper;
import com.visitor.server.service.DashboardService;
import com.visitor.workflow.ApprovalStatuses;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import java.time.LocalDateTime;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@ActiveProfiles("test")
class DashboardServiceTest {

    @Autowired
    private DashboardService service;

    @Autowired
    private VisitorRecordMapper recordMapper;

    @Autowired
    private ApprovalTaskMapper taskMapper;

    @BeforeEach
    void setUp() {
        taskMapper.delete(null);
        recordMapper.delete(null);
    }

    @Test
    void summaryCountsRecordsAndTasks() {
        insertRecord(0);
        insertRecord(1);
        insertRecord(3);
        insertRecord(4);

        ApprovalTask task = new ApprovalTask();
        task.setInstanceId(1L);
        task.setSeq(1);
        task.setNodeName("审批");
        task.setNodeCode("TARGET");
        task.setAssigneeType("TARGET");
        task.setStatus(ApprovalStatuses.PENDING);
        task.setDueAt(LocalDateTime.now().plusHours(1));
        taskMapper.insert(task);

        Map<String, Object> summary = service.summary();

        assertThat(number(summary.get("total"))).isEqualTo(4);
        assertThat(number(summary.get("pending"))).isEqualTo(1);
        assertThat(number(summary.get("inSchool"))).isEqualTo(1);
        assertThat(number(summary.get("left"))).isEqualTo(1);
        assertThat(number(summary.get("pendingTasks"))).isEqualTo(1);
        assertThat(number(summary.get("today"))).isEqualTo(4);
    }

    private void insertRecord(int status) {
        VisitorRecord record = new VisitorRecord();
        record.setSchoolId(1L);
        record.setVisitorUserId(1L);
        record.setVisitorName("访客");
        record.setVisitorType("NORMAL");
        record.setExpectedTime(LocalDateTime.now().withHour(10));
        record.setLeaveTime(LocalDateTime.now().plusHours(2));
        record.setStatus(status);
        record.setCreatedAt(LocalDateTime.now());
        record.setUpdatedAt(LocalDateTime.now());
        recordMapper.insert(record);
    }

    private long number(Object value) {
        return ((Number) value).longValue();
    }
}
