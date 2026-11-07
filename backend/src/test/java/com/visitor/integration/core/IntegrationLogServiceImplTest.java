package com.visitor.integration.core;

import com.visitor.common.core.PageResult;
import com.visitor.domain.entity.IntegrationLog;
import com.visitor.server.mapper.IntegrationLogMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@ActiveProfiles("test")
class IntegrationLogServiceImplTest {

    @Autowired
    private IntegrationLogService logService;

    @Autowired
    private IntegrationLogMapper logMapper;

    @BeforeEach
    void clean() {
        logMapper.delete(null);
    }

    @Test
    void recordFillsDefaultFields() {
        IntegrationLog entry = new IntegrationLog();
        entry.setIntegrationKey("SSO");
        entry.setAction("GET /health");

        logService.record(entry);

        assertThat(logMapper.selectCount(null)).isEqualTo(1);
        IntegrationLog saved = logMapper.selectList(null).get(0);
        assertThat(saved.getCreatedAt()).isNotNull();
        assertThat(saved.getSuccess()).isZero();
        assertThat(saved.getDirection()).isNull();
    }

    @Test
    void pageReturnsNewestFirst() {
        insert("SSO", "action-1");
        insert("SSO", "action-2");
        insert("SSO", "action-3");

        PageResult<IntegrationLog> page = logService.page("SSO", 1, 2);

        assertThat(page.total()).isEqualTo(3);
        assertThat(page.records()).hasSize(2);
        assertThat(page.records().get(0).getAction()).isEqualTo("action-3");
        assertThat(page.records().get(1).getAction()).isEqualTo("action-2");
    }

    @Test
    void pageClampsIllegalParameters() {
        insert("SSO", "action-1");

        PageResult<IntegrationLog> page = logService.page(null, 0, 1000);

        assertThat(page.page()).isEqualTo(1);
        assertThat(page.size()).isEqualTo(100);
        assertThat(page.records()).hasSize(1);
    }

    @Test
    void filtersByIntegrationKey() {
        insert("SSO", "a");
        insert("GATE", "b");

        assertThat(logService.page("GATE", 1, 10).total()).isEqualTo(1);
        assertThat(logService.page(null, 1, 10).total()).isEqualTo(2);
    }

    private void insert(String key, String action) {
        IntegrationLog entry = new IntegrationLog();
        entry.setIntegrationKey(key);
        entry.setAction(action);
        entry.setDirection("OUT");
        entry.setSuccess(1);
        entry.setCostMs(12);
        entry.setCreatedAt(LocalDateTime.now());
        logMapper.insert(entry);
    }
}
