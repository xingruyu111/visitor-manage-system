package com.visitor.integration.todo;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * 统一待办 Mock 实现：仅记录并打印，用于联调与演示。
 * 在集成配置中将 TODO_MSG 的 protocol 设为 MOCK 即可启用。
 */
@Slf4j
@Component
public class MockTodoMessageClient implements TodoMessageClient {

    private final AtomicInteger todoCount = new AtomicInteger();
    private final AtomicInteger messageCount = new AtomicInteger();
    private final List<TodoItem> todos = new ArrayList<>();

    @Override
    public String protocol() {
        return "MOCK";
    }

    @Override
    public boolean isConfigured() {
        return true;
    }

    @Override
    public void pushTodo(TodoItem item) {
        todoCount.incrementAndGet();
        todos.add(item);
        log.info("[MOCK-待办] bizId={}, title={}, assignee={}", item.bizId(), item.title(), item.assigneeId());
    }

    @Override
    public void pushMessage(MessageItem item) {
        messageCount.incrementAndGet();
        log.info("[MOCK-消息] bizId={}, title={}, receivers={}", item.bizId(), item.title(), item.receiverIds());
    }

    public int todoCount() {
        return todoCount.get();
    }

    public int messageCount() {
        return messageCount.get();
    }

    public List<TodoItem> todos() {
        return todos;
    }
}
