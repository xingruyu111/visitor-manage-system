package com.visitor.server.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.visitor.common.exception.BizException;
import com.visitor.common.utils.IdentityValidator;
import com.visitor.domain.entity.FormField;
import com.visitor.server.cache.ConfigCacheService;
import com.visitor.server.mapper.FormFieldMapper;
import com.visitor.server.service.FormFieldService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

@Service
public class FormFieldServiceImpl implements FormFieldService {

    private final FormFieldMapper mapper;
    private final ConfigCacheService cache;

    public FormFieldServiceImpl(FormFieldMapper mapper, ConfigCacheService cache) {
        this.mapper = mapper;
        this.cache = cache;
    }

    @Override
    public List<FormField> list(String visitorType, boolean onlyActive) {
        String scope = visitorType == null || visitorType.isBlank()
                ? "ALL" : visitorType.trim().toUpperCase();
        String cacheKey = "form-fields:" + scope + ":" + onlyActive;
        List<FormField> cached = cache.getList(cacheKey, FormField.class);
        if (cached != null) {
            return cached;
        }
        List<FormField> result = mapper.selectList(new LambdaQueryWrapper<FormField>()
                .eq(visitorType != null && !visitorType.isBlank(), FormField::getVisitorType, visitorType)
                .eq(onlyActive, FormField::getStatus, 1)
                .orderByAsc(FormField::getSortNo)
                .orderByAsc(FormField::getId));
        cache.put(cacheKey, result);
        return result;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public FormField create(FormField body) {
        validateBody(body);
        FormField entity = new FormField();
        entity.setVisitorType(body.getVisitorType().trim().toUpperCase());
        entity.setFieldKey(body.getFieldKey().trim());
        entity.setLabel(body.getLabel().trim());
        entity.setFieldType(body.getFieldType() == null ? "TEXT" : body.getFieldType().toUpperCase());
        entity.setRequired(body.getRequired() == null ? 0 : body.getRequired());
        entity.setOptions(body.getOptions());
        entity.setSortNo(body.getSortNo() == null ? 0 : body.getSortNo());
        entity.setStatus(1);
        entity.setCreatedAt(LocalDateTime.now());
        entity.setUpdatedAt(LocalDateTime.now());
        Long exists = mapper.selectCount(new LambdaQueryWrapper<FormField>()
                .eq(FormField::getVisitorType, entity.getVisitorType())
                .eq(FormField::getFieldKey, entity.getFieldKey()));
        if (exists != null && exists > 0) {
            throw new BizException(400, "该访客类型下字段已存在");
        }
        mapper.insert(entity);
        evict(entity.getVisitorType());
        return entity;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public FormField update(Long id, FormField body) {
        FormField entity = mapper.selectById(id);
        if (entity == null) {
            throw new BizException(404, "字段不存在");
        }
        validateBody(body);
        entity.setLabel(body.getLabel().trim());
        entity.setFieldType(body.getFieldType() == null ? entity.getFieldType() : body.getFieldType().toUpperCase());
        entity.setRequired(body.getRequired() == null ? entity.getRequired() : body.getRequired());
        entity.setOptions(body.getOptions());
        entity.setSortNo(body.getSortNo() == null ? entity.getSortNo() : body.getSortNo());
        if (body.getStatus() != null) {
            entity.setStatus(body.getStatus());
        }
        entity.setUpdatedAt(LocalDateTime.now());
        mapper.updateById(entity);
        evict(entity.getVisitorType());
        return entity;
    }

    @Override
    public void delete(Long id) {
        FormField entity = mapper.selectById(id);
        if (entity == null) {
            throw new BizException(404, "字段不存在");
        }
        mapper.deleteById(id);
        evict(entity.getVisitorType());
    }

    private void evict(String visitorType) {
        cache.evict("form-fields:ALL:true", "form-fields:ALL:false",
                "form-fields:" + visitorType + ":true", "form-fields:" + visitorType + ":false");
    }

    @Override
    public void validate(String visitorType, Map<String, String> extra) {
        List<FormField> fields = list(visitorType, true);
        for (FormField field : fields) {
            String value = extra == null ? null : extra.get(field.getFieldKey());
            if (field.getRequired() != null && field.getRequired() == 1
                    && (value == null || value.isBlank())) {
                throw new BizException(400, "缺少必填项：" + field.getLabel());
            }
            if (value == null || value.isBlank()) {
                continue;
            }
            if ("PHONE".equals(field.getFieldType()) && !IdentityValidator.isPhone(value)) {
                throw new BizException(400, "手机号格式不正确：" + field.getLabel());
            }
            if ("ID_CARD".equals(field.getFieldType()) && !IdentityValidator.isIdCard(value)) {
                throw new BizException(400, "身份证号校验失败：" + field.getLabel());
            }
        }
    }

    private void validateBody(FormField body) {
        if (body.getVisitorType() == null || body.getVisitorType().isBlank()) {
            throw new BizException(400, "访客类型不能为空");
        }
        if (body.getFieldKey() == null || body.getFieldKey().isBlank()) {
            throw new BizException(400, "字段编码不能为空");
        }
        if (body.getLabel() == null || body.getLabel().isBlank()) {
            throw new BizException(400, "字段名称不能为空");
        }
    }
}
