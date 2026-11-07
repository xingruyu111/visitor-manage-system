package com.visitor.sync;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.visitor.domain.entity.AccessList;
import com.visitor.domain.entity.IntegrationConfig;
import com.visitor.integration.sync.AccessListSyncPort;
import com.visitor.integration.sync.MockAccessListSyncClient;
import com.visitor.server.mapper.AccessListMapper;
import com.visitor.server.mapper.IntegrationConfigMapper;
import com.visitor.server.service.AccessListSyncService;
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
class AccessListSyncTest {

    @Autowired
    private AccessListSyncService syncService;

    @Autowired
    private MockAccessListSyncClient mockClient;

    @Autowired
    private AccessListMapper accessListMapper;

    @Autowired
    private IntegrationConfigMapper configMapper;

    @BeforeEach
    void setUp() {
        accessListMapper.delete(null);
        mockClient.clear();
        IntegrationConfig config = configMapper.selectOne(new LambdaQueryWrapper<IntegrationConfig>()
                .eq(IntegrationConfig::getIntegrationKey, "ACCESS_SYNC"));
        if (config == null) {
            config = new IntegrationConfig();
            config.setIntegrationKey("ACCESS_SYNC");
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
    void pullImportsAndIsIdempotent() {
        mockClient.offer(new AccessListSyncPort.Entry("PERSON", "13800009999", 1,
                "外部黑名单", null, LocalDateTime.now().plusDays(30)));

        Map<String, Object> first = syncService.pull();
        assertThat(first.get("count")).isEqualTo(1);
        assertThat(accessListMapper.selectCount(null)).isEqualTo(1L);

        Map<String, Object> second = syncService.pull();
        assertThat(second.get("count")).isEqualTo(1);
        assertThat(accessListMapper.selectCount(null)).isEqualTo(1L);
        assertThat(accessListMapper.selectList(null).get(0).getPersonName()).isEqualTo("外部黑名单");
    }

    @Test
    void pushSendsLocalActiveEntries() {
        AccessList local = new AccessList();
        local.setSchoolId(1L);
        local.setListType(2);
        local.setTargetType("PERSON");
        local.setTargetValue("13800008888");
        local.setStatus(1);
        local.setCreatedAt(LocalDateTime.now());
        local.setUpdatedAt(LocalDateTime.now());
        accessListMapper.insert(local);

        Map<String, Object> result = syncService.push();

        assertThat(result.get("count")).isEqualTo(1);
        assertThat(mockClient.pushed()).hasSize(1);
        assertThat(mockClient.pushed().get(0).targetValue()).isEqualTo("13800008888");
    }

    @Test
    void disabledConfigReturnsZero() {
        IntegrationConfig config = configMapper.selectOne(new LambdaQueryWrapper<IntegrationConfig>()
                .eq(IntegrationConfig::getIntegrationKey, "ACCESS_SYNC"));
        config.setEnabled(0);
        configMapper.updateById(config);

        assertThat(syncService.pull().get("count")).isEqualTo(0);
        assertThat(syncService.push().get("count")).isEqualTo(0);
    }
}
