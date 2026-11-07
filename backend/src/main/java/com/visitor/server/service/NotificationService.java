package com.visitor.server.service;

import com.visitor.common.core.PageResult;
import com.visitor.domain.entity.Notification;

public interface NotificationService {

    void notifyUser(Long userId, String title, String content, String type, String bizType, Long bizId);

    PageResult<Notification> my(Long userId, int page, int size);

    long unread(Long userId);

    void markRead(Long userId, Long id);

    int markAllRead(Long userId);
}
