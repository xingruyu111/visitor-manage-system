package com.visitor.server.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.visitor.common.core.PageResult;
import com.visitor.common.exception.BizException;
import com.visitor.domain.entity.Notification;
import com.visitor.server.mapper.NotificationMapper;
import com.visitor.server.service.NotificationService;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.Objects;

@Service
public class NotificationServiceImpl implements NotificationService {

    private final NotificationMapper mapper;

    public NotificationServiceImpl(NotificationMapper mapper) {
        this.mapper = mapper;
    }

    @Override
    public void notifyUser(Long userId, String title, String content, String type, String bizType, Long bizId) {
        if (userId == null) {
            return;
        }
        Notification notification = new Notification();
        notification.setUserId(userId);
        notification.setTitle(title);
        notification.setContent(content);
        notification.setType(type == null ? "SYSTEM" : type);
        notification.setBizType(bizType);
        notification.setBizId(bizId);
        notification.setIsRead(0);
        notification.setCreatedAt(LocalDateTime.now());
        mapper.insert(notification);
    }

    @Override
    public PageResult<Notification> my(Long userId, int page, int size) {
        int safePage = Math.max(page, 1);
        int safeSize = Math.min(Math.max(size, 1), 100);
        Page<Notification> result = mapper.selectPage(new Page<>(safePage, safeSize),
                new LambdaQueryWrapper<Notification>()
                        .eq(Notification::getUserId, userId)
                        .orderByDesc(Notification::getId));
        return PageResult.of(result.getTotal(), safePage, safeSize, result.getRecords());
    }

    @Override
    public long unread(Long userId) {
        Long count = mapper.selectCount(new LambdaQueryWrapper<Notification>()
                .eq(Notification::getUserId, userId)
                .eq(Notification::getIsRead, 0));
        return count == null ? 0 : count;
    }

    @Override
    public void markRead(Long userId, Long id) {
        Notification notification = mapper.selectById(id);
        if (notification == null || !Objects.equals(notification.getUserId(), userId)) {
            throw new BizException(404, "通知不存在");
        }
        notification.setIsRead(1);
        mapper.updateById(notification);
    }

    @Override
    public int markAllRead(Long userId) {
        Notification update = new Notification();
        update.setIsRead(1);
        return mapper.update(update, new LambdaUpdateWrapper<Notification>()
                .eq(Notification::getUserId, userId)
                .eq(Notification::getIsRead, 0));
    }
}
