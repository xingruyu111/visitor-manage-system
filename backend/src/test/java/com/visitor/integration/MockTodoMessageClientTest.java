package com.visitor.integration;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.visitor.domain.entity.IntegrationConfig;
import com.visitor.integration.todo.MockTodoMessageClient;
import com.visitor.integration.todo.TodoMessageClient;
import com.visitor.server.mapper.IntegrationConfigMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@ActiveProfiles("test")
class MockTodoMessageClientTest {

    @Autowired
    private IntegrationFactory integrationFactory;

    @Autowired
    private IntegrationConfigMapper configMapper;

    @Autowired
    private MockTodoMessageClient mockTodoMessageClient;

    @BeforeEach
    void setUp() {
        IntegrationConfig config = configMapper.selectOne(new LambdaQueryWrapper<IntegrationConfig>()
                .eq(IntegrationConfig::getIntegrationKey, "TODO_MSG"));
        if (config == null) {
            config = new IntegrationConfig();
            config.setIntegrationKey("TODO_MSG");
            config.setEnabled(1);
            config.setProtocol("MOCK");
            config.setStatus(1);
            config.setCreatedAt(java.time.LocalDateTime.now());
            config.setUpdatedAt(java.time.LocalDateTime.now());
            configMapper.insert(config);
        } else {
            config.setEnabled(1);
            config.setProtocol("MOCK");
            configMapper.updateById(config);
        }
    }

    @Test
    void mockProtocolIsSelectedAndRecordsTodos() {
        TodoMessageClient client = integrationFactory.todoMessage();
        assertThat(client).isInstanceOf(MockTodoMessageClient.class);

        int before = mockTodoMessageClient.todoCount();
        client.pushTodo(new TodoMessageClient.TodoItem("1", "审批待办", "有新的访客申请", null, "9"));

        assertThat(mockTodoMessageClient.todoCount()).isEqualTo(before + 1);
        assertThat(mockTodoMessageClient.todos()).anyMatch(item -> "审批待办".equals(item.title()));
        assertThat(client.isConfigured()).isTrue();

        client.pushMessage(new TodoMessageClient.MessageItem("2", "消息", "内容", java.util.List.of("9")));
        assertThat(mockTodoMessageClient.messageCount()).isGreaterThanOrEqualTo(1);
    }

    @Test
    void restProtocolFallsBackToNoopWhenNoImplementation() {
        IntegrationConfig config = configMapper.selectOne(new LambdaQueryWrapper<IntegrationConfig>()
                .eq(IntegrationConfig::getIntegrationKey, "TODO_MSG"));
        config.setProtocol("REST");
        configMapper.updateById(config);

        assertThat(integrationFactory.todoMessage()).isNotInstanceOf(MockTodoMessageClient.class);
    }
}
