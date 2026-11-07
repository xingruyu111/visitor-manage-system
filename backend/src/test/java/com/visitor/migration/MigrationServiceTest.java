package com.visitor.migration;

import com.visitor.domain.dto.VisitorRecordImportRow;
import com.visitor.domain.entity.VisitorRecord;
import com.visitor.server.mapper.VisitorRecordMapper;
import com.visitor.server.service.MigrationService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@ActiveProfiles("test")
class MigrationServiceTest {

    @Autowired
    private MigrationService service;

    @Autowired
    private VisitorRecordMapper recordMapper;

    @BeforeEach
    void setUp() {
        recordMapper.delete(null);
    }

    @Test
    void importsValidRowsAndReportsFailures() {
        VisitorRecordImportRow ok = row("张三", "2024-05-01 09:00:00", "2024-05-01 11:00:00", "已离校");
        VisitorRecordImportRow okDateOnly = row("李四", "2024-06-01", "2024-06-01 12:00:00", "已通过");
        VisitorRecordImportRow missingName = row("", "2024-07-01 09:00:00", "2024-07-01 11:00:00", "已通过");
        VisitorRecordImportRow badTime = row("王五", "2024/07/01 09:00", "2024-07-01 11:00:00", "已通过");

        Map<String, Object> result = service.importVisitorRecords(
                List.of(ok, okDateOnly, missingName, badTime));

        assertThat(result.get("success")).isEqualTo(2);
        assertThat(result.get("failed")).isEqualTo(2);
        assertThat(recordMapper.selectCount(null)).isEqualTo(2L);
        VisitorRecord first = recordMapper.selectList(null).stream()
                .filter(item -> "张三".equals(item.getVisitorName())).findFirst().orElseThrow();
        assertThat(first.getStatus()).isEqualTo(4);
    }

    @Test
    void unknownStatusFallsBackToApproved() {
        Map<String, Object> result = service.importVisitorRecords(List.of(
                row("赵六", "2024-08-01 09:00:00", "2024-08-01 10:00:00", "未知状态")));

        assertThat(result.get("success")).isEqualTo(1);
        assertThat(recordMapper.selectList(null).get(0).getStatus()).isEqualTo(1);
    }

    private VisitorRecordImportRow row(String name, String expected, String leave, String status) {
        VisitorRecordImportRow row = new VisitorRecordImportRow();
        row.setVisitorName(name);
        row.setVisitorPhone("13900000000");
        row.setExpectedTime(expected);
        row.setLeaveTime(leave);
        row.setStatus(status);
        return row;
    }
}
