package com.visitor.integration.todo;

import java.util.List;

/**
 * 统一待办与统一消息。
 */
public interface TodoMessageClient {

    boolean isConfigured();

    void pushTodo(TodoItem item);

    void pushMessage(MessageItem item);

    record TodoItem(String bizId, String title, String content, String url, String assigneeId) {
    }

    record MessageItem(String bizId, String title, String content, List<String> receiverIds) {
    }
}
