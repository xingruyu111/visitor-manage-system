package com.visitor.server.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.visitor.common.exception.BizException;
import com.visitor.domain.entity.SystemSetting;
import com.visitor.server.mapper.SystemSettingMapper;
import com.visitor.server.service.SystemSettingService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;

@Service
public class SystemSettingServiceImpl implements SystemSettingService {

    private final SystemSettingMapper mapper;

    public SystemSettingServiceImpl(SystemSettingMapper mapper) {
        this.mapper = mapper;
    }

    @Override
    public List<SystemSetting> list() {
        return mapper.selectList(new LambdaQueryWrapper<SystemSetting>()
                .orderByAsc(SystemSetting::getSettingKey));
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public SystemSetting update(String key, String value) {
        SystemSetting setting = mapper.selectOne(new LambdaQueryWrapper<SystemSetting>()
                .eq(SystemSetting::getSettingKey, key));
        if (setting == null) {
            throw new BizException(404, "参数不存在");
        }
        setting.setSettingValue(value);
        setting.setUpdatedAt(LocalDateTime.now());
        mapper.updateById(setting);
        return setting;
    }

    @Override
    public String get(String key, String defaultValue) {
        SystemSetting setting = mapper.selectOne(new LambdaQueryWrapper<SystemSetting>()
                .eq(SystemSetting::getSettingKey, key));
        return setting == null || setting.getSettingValue() == null ? defaultValue : setting.getSettingValue();
    }

    @Override
    public int getInt(String key, int defaultValue) {
        try {
            return Integer.parseInt(get(key, String.valueOf(defaultValue)).trim());
        } catch (NumberFormatException e) {
            return defaultValue;
        }
    }

    @Override
    public boolean getBool(String key, boolean defaultValue) {
        return Boolean.parseBoolean(get(key, String.valueOf(defaultValue)).trim());
    }

    @Override
    public List<String> getList(String key, String defaultValue) {
        String value = get(key, defaultValue);
        return Arrays.stream(value.split(",")).map(String::trim).filter(item -> !item.isEmpty()).toList();
    }
}
