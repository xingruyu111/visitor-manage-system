package com.visitor.server.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.visitor.domain.entity.AccessList;
import com.visitor.integration.IntegrationKey;
import com.visitor.integration.config.IntegrationConfigService;
import com.visitor.integration.sync.AccessListSyncPort;
import com.visitor.integration.sync.MockAccessListSyncClient;
import com.visitor.server.mapper.AccessListMapper;
import com.visitor.server.service.AccessCheckService;
import com.visitor.server.service.AccessListSyncService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Slf4j
@Service
public class AccessListSyncServiceImpl implements AccessListSyncService {

    private final IntegrationConfigService configService;
    private final MockAccessListSyncClient mockClient;
    private final AccessListMapper accessListMapper;
    private final AccessCheckService accessCheckService;

    public AccessListSyncServiceImpl(IntegrationConfigService configService,
                                     MockAccessListSyncClient mockClient,
                                     AccessListMapper accessListMapper,
                                     AccessCheckService accessCheckService) {
        this.configService = configService;
        this.mockClient = mockClient;
        this.accessListMapper = accessListMapper;
        this.accessCheckService = accessCheckService;
    }

    @Override
    public Map<String, Object> pull() {
        AccessListSyncPort port = port();
        if (port == null) {
            return result(0, "名单同步未启用");
        }
        int applied = 0;
        for (AccessListSyncPort.Entry entry : port.pull()) {
            AccessList existing = accessListMapper.selectOne(new LambdaQueryWrapper<AccessList>()
                    .eq(AccessList::getSchoolId, 1L)
                    .eq(AccessList::getListType, entry.listType())
                    .eq(AccessList::getTargetType, entry.targetType())
                    .eq(AccessList::getTargetValue, entry.targetValue())
                    .last("LIMIT 1"));
            if (existing != null) {
                existing.setPersonName(entry.personName());
                existing.setPlateNumber(entry.plateNumber());
                existing.setEffectiveTo(entry.effectiveTo());
                existing.setStatus(1);
                existing.setUpdatedAt(LocalDateTime.now());
                accessListMapper.updateById(existing);
                applied++;
                continue;
            }
            AccessList body = new AccessList();
            body.setListType(entry.listType());
            body.setTargetType(entry.targetType());
            body.setTargetValue(entry.targetValue());
            body.setPersonName(entry.personName());
            body.setPlateNumber(entry.plateNumber());
            body.setEffectiveTo(entry.effectiveTo());
            try {
                accessCheckService.create(1L, body);
                applied++;
            } catch (Exception e) {
                log.warn("名单同步跳过异常条目: value={}, err={}", entry.targetValue(), e.getMessage());
            }
        }
        Map<String, Object> result = result(applied, "拉取完成");
        result.put("protocol", port.protocol());
        return result;
    }

    @Override
    public Map<String, Object> push() {
        AccessListSyncPort port = port();
        if (port == null) {
            return result(0, "名单同步未启用");
        }
        List<AccessList> local = accessListMapper.selectList(new LambdaQueryWrapper<AccessList>()
                .eq(AccessList::getStatus, 1));
        List<AccessListSyncPort.Entry> entries = new ArrayList<>();
        for (AccessList item : local) {
            entries.add(new AccessListSyncPort.Entry(item.getTargetType(), item.getTargetValue(),
                    item.getListType(), item.getPersonName(), item.getPlateNumber(), item.getEffectiveTo()));
        }
        port.push(entries);
        Map<String, Object> result = result(entries.size(), "推送完成");
        result.put("protocol", port.protocol());
        return result;
    }

    private AccessListSyncPort port() {
        var config = configService.getEnabled(IntegrationKey.ACCESS_SYNC);
        if (config == null) {
            return null;
        }
        if ("MOCK".equalsIgnoreCase(config.getProtocol())) {
            return mockClient;
        }
        log.warn("名单同步协议 {} 暂无实现", config.getProtocol());
        return null;
    }

    private Map<String, Object> result(int count, String message) {
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("count", count);
        result.put("message", message);
        return result;
    }
}
