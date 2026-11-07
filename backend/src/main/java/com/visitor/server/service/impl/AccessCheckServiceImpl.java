package com.visitor.server.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.visitor.common.core.PageResult;
import com.visitor.common.exception.BizException;
import com.visitor.domain.entity.AccessList;
import com.visitor.domain.dto.AccessListImportRow;
import com.visitor.server.mapper.AccessListMapper;
import com.visitor.server.service.AccessCheckService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.ArrayList;
import java.util.Map;
import java.time.format.DateTimeFormatter;

@Service
public class AccessCheckServiceImpl implements AccessCheckService {

    private static final int BLACK = 1;
    private static final int WHITE = 2;

    private final AccessListMapper mapper;
    private final GateSyncService gateSyncService;

    public AccessCheckServiceImpl(AccessListMapper mapper, GateSyncService gateSyncService) {
        this.mapper = mapper;
        this.gateSyncService = gateSyncService;
    }

    @Override
    public String check(Long schoolId, String phone, String idCard, String plate) {
        List<String> values = new ArrayList<>();
        if (phone != null && !phone.isBlank()) {
            values.add(phone);
        }
        if (idCard != null && !idCard.isBlank()) {
            values.add(idCard);
        }
        if (plate != null && !plate.isBlank()) {
            values.add(plate.toUpperCase());
        }
        if (values.isEmpty()) {
            return "NONE";
        }
        LocalDateTime now = LocalDateTime.now();
        List<AccessList> hits = mapper.selectList(new LambdaQueryWrapper<AccessList>()
                .eq(AccessList::getSchoolId, schoolId)
                .eq(AccessList::getStatus, 1)
                .in(AccessList::getTargetValue, values)
                .and(w -> w.isNull(AccessList::getEffectiveFrom).or().le(AccessList::getEffectiveFrom, now))
                .and(w -> w.isNull(AccessList::getEffectiveTo).or().ge(AccessList::getEffectiveTo, now)));
        if (hits.stream().anyMatch(item -> item.getListType() == BLACK)) {
            return "BLACKLIST";
        }
        if (hits.stream().anyMatch(item -> item.getListType() == WHITE)) {
            return "WHITELIST";
        }
        return "NONE";
    }

    @Override
    public PageResult<AccessList> page(Long schoolId, Integer listType, String targetType,
                                       String keyword, int page, int size) {
        int safePage = Math.max(page, 1);
        int safeSize = Math.min(Math.max(size, 1), 100);
        Page<AccessList> result = mapper.selectPage(new Page<>(safePage, safeSize),
                new LambdaQueryWrapper<AccessList>()
                        .eq(AccessList::getSchoolId, schoolId)
                        .eq(listType != null, AccessList::getListType, listType)
                        .eq(targetType != null && !targetType.isBlank(), AccessList::getTargetType, targetType)
                        .and(keyword != null && !keyword.isBlank(), w -> w
                                .like(AccessList::getTargetValue, keyword)
                                .or().like(AccessList::getPersonName, keyword))
                        .orderByDesc(AccessList::getId));
        return PageResult.of(result.getTotal(), safePage, safeSize, result.getRecords());
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public AccessList create(Long schoolId, AccessList body) {
        validate(body);
        AccessList entity = new AccessList();
        entity.setSchoolId(schoolId);
        entity.setListType(body.getListType());
        entity.setTargetType(body.getTargetType().trim().toUpperCase());
        entity.setTargetValue(normalize(body.getTargetType(), body.getTargetValue()));
        entity.setPersonName(body.getPersonName());
        entity.setPlateNumber(body.getPlateNumber());
        entity.setEffectiveFrom(body.getEffectiveFrom());
        entity.setEffectiveTo(body.getEffectiveTo());
        entity.setReason(body.getReason());
        entity.setSource("MANUAL");
        entity.setStatus(1);
        entity.setCreatedAt(LocalDateTime.now());
        entity.setUpdatedAt(LocalDateTime.now());
        duplicateCheck(schoolId, entity);
        mapper.insert(entity);
        return entity;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public AccessList update(Long schoolId, Long id, AccessList body) {
        AccessList entity = require(schoolId, id);
        validate(body);
        entity.setListType(body.getListType());
        entity.setTargetType(body.getTargetType().trim().toUpperCase());
        entity.setTargetValue(normalize(body.getTargetType(), body.getTargetValue()));
        entity.setPersonName(body.getPersonName());
        entity.setPlateNumber(body.getPlateNumber());
        entity.setEffectiveFrom(body.getEffectiveFrom());
        entity.setEffectiveTo(body.getEffectiveTo());
        entity.setReason(body.getReason());
        if (body.getStatus() != null) {
            entity.setStatus(body.getStatus());
        }
        entity.setUpdatedAt(LocalDateTime.now());
        mapper.updateById(entity);
        return entity;
    }

    @Override
    public void delete(Long schoolId, Long id) {
        require(schoolId, id);
        mapper.deleteById(id);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public AccessList release(Long schoolId, Long id, String reason) {
        AccessList entity = require(schoolId, id);
        entity.setStatus(0);
        entity.setReleasedAt(LocalDateTime.now());
        entity.setReleaseReason(reason);
        entity.setUpdatedAt(LocalDateTime.now());
        mapper.updateById(entity);
        return entity;
    }

    @Override
    public Map<String, Object> importRows(Long schoolId, List<AccessListImportRow> rows) {
        int success = 0;
        List<Map<String, Object>> errors = new ArrayList<>();
        if (rows == null) {
            return Map.of("success", 0, "failed", 0, "errors", errors);
        }
        for (int i = 0; i < rows.size(); i++) {
            AccessListImportRow row = rows.get(i);
            try {
                AccessList body = toEntity(row);
                create(schoolId, body);
                success++;
            } catch (Exception e) {
                errors.add(Map.of("row", i + 2, "reason", e.getMessage() == null ? "导入失败" : e.getMessage()));
            }
        }
        Map<String, Object> result = new java.util.LinkedHashMap<>();
        result.put("success", success);
        result.put("failed", errors.size());
        result.put("errors", errors);
        return result;
    }

    private AccessList toEntity(AccessListImportRow row) {
        boolean hasPlate = row.getPlateNumber() != null && !row.getPlateNumber().isBlank();
        boolean hasPhone = row.getPhone() != null && !row.getPhone().isBlank();
        if (!hasPlate && !hasPhone) {
            throw new BizException(400, "手机号与车牌号至少填一个");
        }
        AccessList body = new AccessList();
        body.setListType(row.getListType() != null && row.getListType().contains("白") ? WHITE : BLACK);
        body.setTargetType(hasPlate ? "PLATE_MOTOR" : "PERSON");
        body.setTargetValue(hasPlate ? row.getPlateNumber() : row.getPhone());
        body.setPersonName(row.getPersonName());
        body.setPlateNumber(row.getPlateNumber());
        body.setReason(row.getReason());
        if (row.getEffectiveTo() != null && !row.getEffectiveTo().isBlank()) {
            body.setEffectiveTo(parseTime(row.getEffectiveTo().trim()));
        }
        return body;
    }

    private LocalDateTime parseTime(String value) {
        try {
            if (value.length() == 10) {
                return java.time.LocalDate.parse(value).atTime(23, 59, 59);
            }
            return LocalDateTime.parse(value, DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));
        } catch (Exception e) {
            throw new BizException(400, "失效时间格式应为 yyyy-MM-dd 或 yyyy-MM-dd HH:mm:ss");
        }
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public int expireOverdue() {
        AccessList update = new AccessList();
        update.setStatus(0);
        update.setUpdatedAt(LocalDateTime.now());
        return mapper.update(update, new LambdaQueryWrapper<AccessList>()
                .eq(AccessList::getStatus, 1)
                .lt(AccessList::getEffectiveTo, LocalDateTime.now()));
    }

    private void validate(AccessList body) {
        if (body.getListType() == null || (body.getListType() != BLACK && body.getListType() != WHITE)) {
            throw new BizException(400, "名单类型不合法");
        }
        if (body.getTargetType() == null || body.getTargetType().isBlank()) {
            throw new BizException(400, "对象类型不能为空");
        }
        if (body.getTargetValue() == null || body.getTargetValue().isBlank()) {
            throw new BizException(400, "对象值不能为空");
        }
        if (body.getEffectiveFrom() != null && body.getEffectiveTo() != null
                && !body.getEffectiveTo().isAfter(body.getEffectiveFrom())) {
            throw new BizException(400, "失效时间必须晚于生效时间");
        }
    }

    private void duplicateCheck(Long schoolId, AccessList entity) {
        Long count = mapper.selectCount(new LambdaQueryWrapper<AccessList>()
                .eq(AccessList::getSchoolId, schoolId)
                .eq(AccessList::getListType, entity.getListType())
                .eq(AccessList::getTargetType, entity.getTargetType())
                .eq(AccessList::getTargetValue, entity.getTargetValue()));
        if (count != null && count > 0) {
            throw new BizException(400, "该对象已在本名单中");
        }
    }

    private String normalize(String targetType, String value) {
        String trimmed = value.trim();
        return targetType.toUpperCase().startsWith("PLATE") ? trimmed.toUpperCase() : trimmed;
    }

    private AccessList require(Long schoolId, Long id) {
        AccessList entity = mapper.selectById(id);
        if (entity == null || !Objects.equals(entity.getSchoolId(), schoolId)) {
            throw new BizException(404, "名单记录不存在");
        }
        return entity;
    }
}
