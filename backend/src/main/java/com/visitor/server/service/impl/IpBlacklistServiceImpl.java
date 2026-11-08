package com.visitor.server.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.visitor.common.core.PageResult;
import com.visitor.common.exception.BizException;
import com.visitor.domain.entity.IpBlacklist;
import com.visitor.server.mapper.IpBlacklistMapper;
import com.visitor.server.service.IpBlacklistService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Service
public class IpBlacklistServiceImpl implements IpBlacklistService {

    private final IpBlacklistMapper mapper;

    public IpBlacklistServiceImpl(IpBlacklistMapper mapper) {
        this.mapper = mapper;
    }

    @Override
    public PageResult<IpBlacklist> page(String keyword, int page, int size) {
        int safePage = Math.max(page, 1);
        int safeSize = Math.min(Math.max(size, 1), 100);
        Page<IpBlacklist> result = mapper.selectPage(new Page<>(safePage, safeSize),
                new LambdaQueryWrapper<IpBlacklist>()
                        .like(keyword != null && !keyword.isBlank(), IpBlacklist::getIp, keyword)
                        .orderByDesc(IpBlacklist::getId));
        return PageResult.of(result.getTotal(), safePage, safeSize, result.getRecords());
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public IpBlacklist add(String ip, String reason, LocalDateTime expireAt, Long operatorId) {
        if (ip == null || ip.isBlank()) {
            throw new BizException(400, "IP 不能为空");
        }
        String value = ip.trim();
        IpBlacklist existing = mapper.selectOne(new LambdaQueryWrapper<IpBlacklist>()
                .eq(IpBlacklist::getIp, value));
        if (existing != null) {
            throw new BizException(400, "该 IP 已在黑名单中");
        }
        IpBlacklist entity = new IpBlacklist();
        entity.setIp(value);
        entity.setReason(reason);
        entity.setExpireAt(expireAt);
        entity.setCreatedBy(operatorId);
        entity.setCreatedAt(LocalDateTime.now());
        mapper.insert(entity);
        return entity;
    }

    @Override
    public void remove(Long id) {
        if (mapper.selectById(id) == null) {
            throw new BizException(404, "记录不存在");
        }
        mapper.deleteById(id);
    }

    @Override
    public void assertAllowed(String ip) {
        if (ip == null || ip.isBlank()) {
            return;
        }
        IpBlacklist entry = mapper.selectOne(new LambdaQueryWrapper<IpBlacklist>()
                .eq(IpBlacklist::getIp, ip.trim())
                .last("LIMIT 1"));
        if (entry == null) {
            return;
        }
        if (entry.getExpireAt() != null && entry.getExpireAt().isBefore(LocalDateTime.now())) {
            return;
        }
        throw new BizException(429, "该 IP 已被禁止访问");
    }
}
