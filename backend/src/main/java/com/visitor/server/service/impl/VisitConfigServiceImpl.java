package com.visitor.server.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.visitor.common.exception.BizException;
import com.visitor.domain.entity.VisitConfig;
import com.visitor.server.mapper.VisitConfigMapper;
import com.visitor.server.service.VisitConfigService;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

@Service
public class VisitConfigServiceImpl implements VisitConfigService {

    private static final Long SCHOOL_ID = 1L;

    private final VisitConfigMapper mapper;
    private final ObjectMapper objectMapper;

    public VisitConfigServiceImpl(VisitConfigMapper mapper, ObjectMapper objectMapper) {
        this.mapper = mapper;
        this.objectMapper = objectMapper;
    }

    @Override
    public List<Integer> alumniMonths() {
        VisitConfig config = getOrCreate();
        if (config.getAlumniMonthsJson() == null || config.getAlumniMonthsJson().isBlank()) {
            return List.of();
        }
        try {
            return objectMapper.readValue(config.getAlumniMonthsJson(), new TypeReference<List<Integer>>() {
            });
        } catch (Exception e) {
            return List.of();
        }
    }

    @Override
    public void updateAlumniMonths(List<Integer> months) {
        Set<Integer> unique = new LinkedHashSet<>();
        if (months != null) {
            for (Integer month : months) {
                if (month == null || month < 1 || month > 12) {
                    throw new BizException(400, "通行月份必须在 1-12 之间");
                }
                unique.add(month);
            }
        }
        VisitConfig config = getOrCreate();
        try {
            config.setAlumniMonthsJson(objectMapper.writeValueAsString(new ArrayList<>(unique)));
        } catch (Exception e) {
            throw new BizException(400, "校友通行月份保存失败");
        }
        config.setUpdatedAt(LocalDateTime.now());
        mapper.updateById(config);
    }

    private VisitConfig getOrCreate() {
        VisitConfig config = mapper.selectOne(new LambdaQueryWrapper<VisitConfig>()
                .eq(VisitConfig::getSchoolId, SCHOOL_ID)
                .last("LIMIT 1"));
        if (config == null) {
            config = new VisitConfig();
            config.setSchoolId(SCHOOL_ID);
            config.setAlumniMonthsJson("[]");
            config.setCreatedAt(LocalDateTime.now());
            config.setUpdatedAt(LocalDateTime.now());
            mapper.insert(config);
        }
        return config;
    }
}
