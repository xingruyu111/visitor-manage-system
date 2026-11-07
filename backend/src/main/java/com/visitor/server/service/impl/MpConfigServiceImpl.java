package com.visitor.server.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.visitor.domain.entity.MpConfig;
import com.visitor.server.mapper.MpConfigMapper;
import com.visitor.server.service.MpConfigService;
import org.springframework.stereotype.Service;

@Service
public class MpConfigServiceImpl implements MpConfigService {

    private final MpConfigMapper mapper;

    public MpConfigServiceImpl(MpConfigMapper mapper) {
        this.mapper = mapper;
    }

    @Override
    public MpConfig get() {
        return mapper.selectOne(new LambdaQueryWrapper<MpConfig>().last("LIMIT 1"));
    }
}
