package com.visitor.integration.todo;

import com.visitor.integration.core.IntegrationNotConfiguredException;

/**
 * 未配置统一待办/消息时的兜底实现：回落站内通知与服务号模板消息。
 */
public final class NoopTodoMessageClient implements TodoMessageClient {

    public static final NoopTodoMessageClient INSTANCE = new NoopTodoMessageClient();
    private static final String KEY = "TODO_MSG";

    private NoopTodoMessageClient() {
    }

    @Override
    public boolean isConfigured() {
        return false;
    }

    @Override
    public void pushTodo(TodoItem item) {
        throw new IntegrationNotConfiguredException(KEY);
    }

    @Override
    public void pushMessage(MessageItem item) {
        throw new IntegrationNotConfiguredException(KEY);
    }
}
