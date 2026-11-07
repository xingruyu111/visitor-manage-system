package com.visitor.integration.core;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.visitor.common.core.PageResult;
import com.visitor.domain.entity.IntegrationLog;
import com.visitor.server.mapper.IntegrationLogMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;

@Service
public class IntegrationLogServiceImpl implements IntegrationLogService {

    private static final Logger log = LoggerFactory.getLogger(IntegrationLogServiceImpl.class);

    private final IntegrationLogMapper mapper;

    public IntegrationLogServiceImpl(IntegrationLogMapper mapper) {
        this.mapper = mapper;
    }

    @Override
    public void record(IntegrationLog entry) {
        try {
            if (entry.getCreatedAt() == null) {
                entry.setCreatedAt(LocalDateTime.now());
            }
            if (entry.getSuccess() == null) {
                entry.setSuccess(0);
            }
            mapper.insert(entry);
        } catch (Exception e) {
            log.warn("集成日志写入失败: key={}, action={}, error={}",
                    entry.getIntegrationKey(), entry.getAction(), e.getMessage());
        }
    }

    @Override
    public PageResult<IntegrationLog> page(String integrationKey, int page, int size) {
        int safePage = Math.max(page, 1);
        int safeSize = Math.min(Math.max(size, 1), 100);

        LambdaQueryWrapper<IntegrationLog> wrapper = new LambdaQueryWrapper<IntegrationLog>()
                .eq(integrationKey != null && !integrationKey.isBlank(), IntegrationLog::getIntegrationKey, integrationKey)
                .orderByDesc(IntegrationLog::getId);

        Page<IntegrationLog> result = mapper.selectPage(new Page<>(safePage, safeSize), wrapper);
        return PageResult.of(result.getTotal(), safePage, safeSize, result.getRecords());
    }
}
