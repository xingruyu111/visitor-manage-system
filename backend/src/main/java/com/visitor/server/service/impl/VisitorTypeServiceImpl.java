package com.visitor.server.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.visitor.common.exception.BizException;
import com.visitor.domain.dto.BookingRule;
import com.visitor.domain.dto.VisitorTypeSaveDTO;
import com.visitor.domain.entity.VisitorType;
import com.visitor.server.cache.ConfigCacheService;
import com.visitor.server.mapper.FormFieldMapper;
import com.visitor.server.mapper.VisitorTypeMapper;
import com.visitor.server.service.VisitorTypeService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Set;

@Service
public class VisitorTypeServiceImpl implements VisitorTypeService {

    private static final Set<String> MODES = Set.of("DAILY", "WEEKLY", "MONTHLY", "MONTH_DAY", "UNLIMITED");
    private static final DateTimeFormatter TIME = DateTimeFormatter.ofPattern("HH:mm");

    private final VisitorTypeMapper mapper;
    private final ObjectMapper objectMapper;
    private final FormFieldMapper formFieldMapper;
    private final ConfigCacheService cache;

    private static final String KEY_ALL = "visitor-types:all";
    private static final String KEY_ENABLED = "visitor-types:enabled";

    public VisitorTypeServiceImpl(VisitorTypeMapper mapper, ObjectMapper objectMapper,
                                  FormFieldMapper formFieldMapper, ConfigCacheService cache) {
        this.mapper = mapper;
        this.objectMapper = objectMapper;
        this.formFieldMapper = formFieldMapper;
        this.cache = cache;
    }

    @Override
    public List<VisitorType> list(boolean onlyEnabled) {
        String cacheKey = onlyEnabled ? KEY_ENABLED : KEY_ALL;
        List<VisitorType> cached = cache.getList(cacheKey, VisitorType.class);
        if (cached != null) {
            return cached;
        }
        List<VisitorType> result = mapper.selectList(new LambdaQueryWrapper<VisitorType>()
                .eq(onlyEnabled, VisitorType::getStatus, 1)
                .orderByAsc(VisitorType::getSortNo)
                .orderByAsc(VisitorType::getId));
        cache.put(cacheKey, result);
        return result;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public VisitorType create(VisitorTypeSaveDTO dto) {
        String code = normalizeCode(dto.getCode());
        Long exists = mapper.selectCount(new LambdaQueryWrapper<VisitorType>().eq(VisitorType::getCode, code));
        if (exists != null && exists > 0) {
            throw new BizException(400, "类型编码已存在");
        }
        VisitorType entity = new VisitorType();
        apply(entity, dto, code);
        entity.setCreatedAt(LocalDateTime.now());
        entity.setUpdatedAt(LocalDateTime.now());
        mapper.insert(entity);
        evictType(code);
        return entity;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public VisitorType update(Long id, VisitorTypeSaveDTO dto) {
        VisitorType entity = get(id);
        String oldCode = entity.getCode();
        String code = normalizeCode(dto.getCode());
        Long exists = mapper.selectCount(new LambdaQueryWrapper<VisitorType>()
                .eq(VisitorType::getCode, code)
                .ne(VisitorType::getId, id));
        if (exists != null && exists > 0) {
            throw new BizException(400, "类型编码已存在");
        }
        apply(entity, dto, code);
        entity.setUpdatedAt(LocalDateTime.now());
        mapper.updateById(entity);
        evictType(oldCode, code);
        return entity;
    }

    @Override
    public VisitorType updateStatus(Long id, Integer status) {
        VisitorType entity = get(id);
        if (status == null || (status != 0 && status != 1)) {
            throw new BizException(400, "状态不合法");
        }
        entity.setStatus(status);
        entity.setUpdatedAt(LocalDateTime.now());
        mapper.updateById(entity);
        evictType(entity.getCode());
        return entity;
    }

    @Override
    public void delete(Long id) {
        VisitorType entity = get(id);
        if (isBuiltin(entity.getCode())) {
            throw new BizException(400, "内置访客类型不能删除，可以停用");
        }
        Long used = formFieldMapper.selectCount(new LambdaQueryWrapper<com.visitor.domain.entity.FormField>()
                .eq(com.visitor.domain.entity.FormField::getVisitorType, entity.getCode()));
        if (used != null && used > 0) {
            throw new BizException(400, "该类型已有表单字段，不能删除");
        }
        mapper.deleteById(id);
        evictType(entity.getCode());
    }

    private void evictType(String... codes) {
        java.util.List<String> keys = new java.util.ArrayList<>(java.util.List.of(KEY_ALL, KEY_ENABLED));
        for (String code : codes) {
            if (code != null) {
                keys.add("visitor-type:rules:" + code);
                keys.add("visitor-type:max:" + code);
            }
        }
        cache.evict(keys.toArray(String[]::new));
    }

    private boolean isBuiltin(String code) {
        return Set.of("NORMAL", "PARENT", "ALUMNI", "CONSTRUCTION").contains(code);
    }

    @Override
    public List<BookingRule> rulesOf(String code) {
        if (code == null || code.isBlank()) {
            return List.of();
        }
        String normalized = code.trim().toUpperCase(Locale.ROOT);
        String cacheKey = "visitor-type:rules:" + normalized;
        List<BookingRule> cached = cache.getList(cacheKey, BookingRule.class);
        if (cached != null) {
            return cached;
        }
        VisitorType type = mapper.selectOne(new LambdaQueryWrapper<VisitorType>()
                .eq(VisitorType::getCode, normalized)
                .last("LIMIT 1"));
        if (type == null || type.getBookingRulesJson() == null || type.getBookingRulesJson().isBlank()) {
            cache.put(cacheKey, List.of());
            return List.of();
        }
        try {
            List<BookingRule> rules = objectMapper.readValue(type.getBookingRulesJson(),
                    new TypeReference<List<BookingRule>>() {
                    });
            cache.put(cacheKey, rules);
            return rules;
        } catch (Exception e) {
            throw new BizException(500, "访客类型可预约时间段配置有误");
        }
    }

    @Override
    public void validateBookingTime(String code, LocalDateTime expected, LocalDateTime leave) {
        List<BookingRule> rules = rulesOf(code);
        if (rules.isEmpty() || rules.stream().anyMatch(rule -> "UNLIMITED".equalsIgnoreCase(rule.getMode())
                && isWithinTime(rule, expected, leave, true))) {
            return;
        }
        LocalDate date = expected.toLocalDate();
        if (!date.equals(leave.toLocalDate())) {
            throw new BizException(400, "当前访客类型只能预约同一天内的时段");
        }
        for (BookingRule rule : rules) {
            if (matchesDate(rule, date) && isWithinTime(rule, expected, leave, false)) {
                return;
            }
        }
        throw new BizException(400, "当前访客类型不在可预约时间段内");
    }

    @Override
    public int passMaxEntries(String code) {
        if (code == null || code.isBlank()) {
            return 1;
        }
        String normalized = code.trim().toUpperCase(Locale.ROOT);
        String cacheKey = "visitor-type:max:" + normalized;
        Integer cached = cache.getObject(cacheKey, Integer.class);
        if (cached != null) {
            return cached;
        }
        VisitorType type = mapper.selectOne(new LambdaQueryWrapper<VisitorType>()
                .eq(VisitorType::getCode, normalized)
                .last("LIMIT 1"));
        int result;
        if (type != null && type.getMaxEntries() != null) {
            result = type.getMaxEntries() == 0 ? 999999 : type.getMaxEntries();
        } else {
            result = "ALUMNI".equalsIgnoreCase(code) ? 999999 : 1;
        }
        cache.put(cacheKey, result);
        return result;
    }

    private void apply(VisitorType entity, VisitorTypeSaveDTO dto, String code) {
        if (dto.getMaxEntries() == null || dto.getMaxEntries() < 0) {
            throw new BizException(400, "可通行次数不能小于 0");
        }
        List<BookingRule> rules = dto.getBookingRules() == null ? List.of() : dto.getBookingRules();
        validateRules(rules);
        entity.setCode(code);
        entity.setName(dto.getName().trim());
        entity.setMaxEntries(dto.getMaxEntries());
        entity.setBookingRulesJson(writeRules(rules));
        entity.setStatus(dto.getStatus() == null ? 1 : dto.getStatus());
        entity.setSortNo(dto.getSortNo() == null ? 0 : dto.getSortNo());
    }

    private void validateRules(List<BookingRule> rules) {
        for (BookingRule rule : rules) {
            String mode = rule.getMode() == null ? "" : rule.getMode().toUpperCase(Locale.ROOT);
            if (!MODES.contains(mode)) {
                throw new BizException(400, "可预约时间段模式不合法");
            }
            rule.setMode(mode);
            if (!"UNLIMITED".equals(mode) && (rule.getValues() == null || rule.getValues().isEmpty())) {
                throw new BizException(400, "请选择可预约日期");
            }
            if ((rule.getStartTime() == null) != (rule.getEndTime() == null)) {
                throw new BizException(400, "请同时填写开始时间和结束时间");
            }
            if (rule.getStartTime() != null) {
                try {
                    LocalTime start = LocalTime.parse(rule.getStartTime(), TIME);
                    LocalTime end = LocalTime.parse(rule.getEndTime(), TIME);
                    if (!end.isAfter(start)) {
                        throw new BizException(400, "结束时间必须晚于开始时间");
                    }
                } catch (Exception e) {
                    if (e instanceof BizException biz) {
                        throw biz;
                    }
                    throw new BizException(400, "时间格式应为 HH:mm");
                }
            }
        }
    }

    private String writeRules(List<BookingRule> rules) {
        try {
            return objectMapper.writeValueAsString(rules);
        } catch (Exception e) {
            throw new BizException(400, "可预约时间段保存失败");
        }
    }

    private boolean matchesDate(BookingRule rule, LocalDate date) {
        String mode = rule.getMode() == null ? "" : rule.getMode().toUpperCase(Locale.ROOT);
        return switch (mode) {
            case "UNLIMITED" -> true;
            case "DAILY" -> contains(rule.getValues(), String.valueOf(date.getDayOfMonth()));
            case "WEEKLY" -> contains(rule.getValues(), String.valueOf(date.getDayOfWeek().getValue()));
            case "MONTHLY" -> contains(rule.getValues(), String.valueOf(date.getMonthValue()));
            case "MONTH_DAY" -> contains(rule.getValues(), date.getMonthValue() + "-" + date.getDayOfMonth());
            default -> false;
        };
    }

    private boolean isWithinTime(BookingRule rule, LocalDateTime expected, LocalDateTime leave, boolean unlimitedDate) {
        if (!unlimitedDate && !expected.toLocalDate().equals(leave.toLocalDate())) {
            return false;
        }
        LocalTime start = rule.getStartTime() == null ? LocalTime.MIN : LocalTime.parse(rule.getStartTime(), TIME);
        LocalTime end = rule.getEndTime() == null ? LocalTime.MAX : LocalTime.parse(rule.getEndTime(), TIME);
        return !expected.toLocalTime().isBefore(start) && !leave.toLocalTime().isAfter(end);
    }

    private boolean contains(List<String> values, String value) {
        return values != null && values.stream().map(String::trim).anyMatch(value::equals);
    }

    private VisitorType get(Long id) {
        VisitorType entity = mapper.selectById(id);
        if (entity == null) {
            throw new BizException(404, "访客类型不存在");
        }
        return entity;
    }

    private String normalizeCode(String code) {
        if (code == null || code.isBlank()) {
            throw new BizException(400, "类型编码不能为空");
        }
        return code.trim().toUpperCase(Locale.ROOT);
    }
}
