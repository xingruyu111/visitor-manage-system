package com.visitor.server.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.visitor.common.exception.BizException;
import com.visitor.domain.entity.NotifyConfig;
import com.visitor.server.mapper.NotifyConfigMapper;
import com.visitor.server.service.NotifyConfigService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Service
public class NotifyConfigServiceImpl implements NotifyConfigService {

    private final NotifyConfigMapper mapper;

    public NotifyConfigServiceImpl(NotifyConfigMapper mapper) {
        this.mapper = mapper;
    }

    @Override
    public List<NotifyConfig> list() {
        return mapper.selectList(new LambdaQueryWrapper<NotifyConfig>().orderByAsc(NotifyConfig::getId));
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public NotifyConfig update(String notifyType, Integer enabled, String channels) {
        NotifyConfig config = mapper.selectOne(new LambdaQueryWrapper<NotifyConfig>()
                .eq(NotifyConfig::getNotifyType, notifyType));
        if (config == null) {
            throw new BizException(404, "通知类型不存在");
        }
        if (enabled != null) {
            config.setEnabled(enabled);
        }
        if (channels != null && !channels.isBlank()) {
            config.setChannels(channels.trim().toUpperCase());
        }
        config.setUpdatedAt(LocalDateTime.now());
        mapper.updateById(config);
        return config;
    }

    @Override
    public boolean isEnabled(String notifyType) {
        NotifyConfig config = mapper.selectOne(new LambdaQueryWrapper<NotifyConfig>()
                .eq(NotifyConfig::getNotifyType, notifyType));
        return config == null || config.getEnabled() == null || config.getEnabled() == 1;
    }
}
