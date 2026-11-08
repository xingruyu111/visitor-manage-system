package com.visitor.dashboard;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.visitor.domain.entity.AuditLog;
import com.visitor.domain.entity.IntegrationLog;
import com.visitor.domain.entity.VisitorRecord;
import com.visitor.server.mapper.AuditLogMapper;
import com.visitor.server.mapper.IntegrationLogMapper;
import com.visitor.server.mapper.VisitorRecordMapper;
import com.visitor.server.service.DashboardService;
import com.visitor.server.service.InterfaceLogService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@ActiveProfiles("test")
class DashboardTrendTest {

    @Autowired
    private DashboardService dashboardService;

    @Autowired
    private InterfaceLogService integrationLogService;

    @Autowired
    private VisitorRecordMapper recordMapper;

    @Autowired
    private AuditLogMapper auditLogMapper;

    @Autowired
    private IntegrationLogMapper integrationLogMapper;

    @BeforeEach
    void setUp() {
        recordMapper.delete(null);
        auditLogMapper.delete(null);
        integrationLogMapper.delete(null);
    }

    @Test
    void trendIncludesDailyCountsRateAndAlerts() {
        record(1);
        record(2);
        record(4);

        AuditLog hit = new AuditLog();
        hit.setAction("BLACKLIST_HIT");
        hit.setDetail("黑名单拦截");
        hit.setCreatedAt(LocalDateTime.now());
        auditLogMapper.insert(hit);

        IntegrationLog log = new IntegrationLog();
        log.setIntegrationKey("GATE");
        log.setAction("pushPermission");
        log.setSuccess(0);
        log.setCreatedAt(LocalDateTime.now());
        integrationLogMapper.insert(log);

        Map<String, Object> trend = dashboardService.trend(7);

        List<?> daily = (List<?>) trend.get("daily");
        assertThat(daily).hasSize(7);
        @SuppressWarnings("unchecked")
        Map<String, Object> today = (Map<String, Object>) daily.get(daily.size() - 1);
        assertThat(((Number) today.get("total")).intValue()).isEqualTo(3);
        assertThat(((Number) today.get("approved")).intValue()).isEqualTo(1);
        assertThat(((Number) today.get("rejected")).intValue()).isEqualTo(1);
        assertThat(((Number) today.get("left")).intValue()).isEqualTo(1);
        assertThat(((Number) trend.get("approvalRate")).doubleValue()).isEqualTo(50.0);
        assertThat(((Number) trend.get("blacklistHits")).intValue()).isEqualTo(1);
        assertThat(((Number) trend.get("integrationErrors")).intValue()).isEqualTo(1);
    }

    @Test
    void integrationLogsCanBeFilteredAndExported() {
        IntegrationLog ok = new IntegrationLog();
        ok.setIntegrationKey("SSO");
        ok.setAction("login");
        ok.setSuccess(1);
        ok.setCreatedAt(LocalDateTime.now());
        integrationLogMapper.insert(ok);

        IntegrationLog failed = new IntegrationLog();
        failed.setIntegrationKey("GATE");
        failed.setAction("pushPermission");
        failed.setSuccess(0);
        failed.setErrorCode("TIMEOUT");
        failed.setCreatedAt(LocalDateTime.now());
        integrationLogMapper.insert(failed);

        assertThat(integrationLogService.page(null, 0, 1, 10).total()).isEqualTo(1);
        assertThat(integrationLogService.page("SSO", null, 1, 10).total()).isEqualTo(1);
        assertThat(integrationLogService.exportRows(null, 0)).hasSize(1);
    }

    @Test
    void monthlyAggregatesCurrentMonth() {
        record(1);
        record(2);

        List<Map<String, Object>> months = dashboardService.monthly(3);

        assertThat(months).hasSize(3);
        Map<String, Object> current = months.get(2);
        assertThat(((Number) current.get("total")).intValue()).isEqualTo(2);
        assertThat(((Number) current.get("approved")).intValue()).isEqualTo(1);
        assertThat(((Number) current.get("rejected")).intValue()).isEqualTo(1);
        assertThat(((Number) current.get("approvalRate")).doubleValue()).isEqualTo(50.0);
    }
    private void record(int status) {
        VisitorRecord record = new VisitorRecord();
        record.setSchoolId(1L);
        record.setVisitorUserId(1L);
        record.setVisitorName("统计访客");
        record.setVisitorType("NORMAL");
        record.setExpectedTime(LocalDateTime.now().withHour(10));
        record.setLeaveTime(LocalDateTime.now().plusHours(2));
        record.setStatus(status);
        record.setCreatedAt(LocalDateTime.now());
        record.setUpdatedAt(LocalDateTime.now());
        recordMapper.insert(record);
    }
}
