package com.visitor.server.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.visitor.common.core.PageResult;
import com.visitor.domain.entity.IntegrationLog;
import com.visitor.server.mapper.IntegrationLogMapper;
import com.visitor.server.service.InterfaceLogService;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class InterfaceLogServiceImpl implements InterfaceLogService {

    private final IntegrationLogMapper mapper;

    public InterfaceLogServiceImpl(IntegrationLogMapper mapper) {
        this.mapper = mapper;
    }

    @Override
    public PageResult<IntegrationLog> page(String integrationKey, Integer success, int page, int size) {
        int safePage = Math.max(page, 1);
        int safeSize = Math.min(Math.max(size, 1), 100);
        Page<IntegrationLog> result = mapper.selectPage(new Page<>(safePage, safeSize),
                wrapper(integrationKey, success));
        return PageResult.of(result.getTotal(), safePage, safeSize, result.getRecords());
    }

    @Override
    public List<IntegrationLog> exportRows(String integrationKey, Integer success) {
        return mapper.selectList(wrapper(integrationKey, success)
                .orderByDesc(IntegrationLog::getId)
                .last("LIMIT 5000"));
    }

    private LambdaQueryWrapper<IntegrationLog> wrapper(String integrationKey, Integer success) {
        return new LambdaQueryWrapper<IntegrationLog>()
                .eq(integrationKey != null && !integrationKey.isBlank(), IntegrationLog::getIntegrationKey, integrationKey)
                .eq(success != null, IntegrationLog::getSuccess, success)
                .orderByDesc(IntegrationLog::getId);
    }
}
