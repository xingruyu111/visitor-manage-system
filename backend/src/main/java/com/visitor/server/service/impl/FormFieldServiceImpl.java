package com.visitor.server.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.visitor.common.exception.BizException;
import com.visitor.domain.entity.FormField;
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

    public FormFieldServiceImpl(FormFieldMapper mapper) {
        this.mapper = mapper;
    }

    @Override
    public List<FormField> list(String visitorType, boolean onlyActive) {
        return mapper.selectList(new LambdaQueryWrapper<FormField>()
                .eq(visitorType != null && !visitorType.isBlank(), FormField::getVisitorType, visitorType)
                .eq(onlyActive, FormField::getStatus, 1)
                .orderByAsc(FormField::getSortNo)
                .orderByAsc(FormField::getId));
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
        return entity;
    }

    @Override
    public void delete(Long id) {
        if (mapper.selectById(id) == null) {
            throw new BizException(404, "字段不存在");
        }
        mapper.deleteById(id);
    }

    @Override
    public void validate(String visitorType, Map<String, String> extra) {
        List<FormField> fields = list(visitorType, true);
        for (FormField field : fields) {
            if (field.getRequired() != null && field.getRequired() == 1) {
                String value = extra == null ? null : extra.get(field.getFieldKey());
                if (value == null || value.isBlank()) {
                    throw new BizException(400, "缺少必填项：" + field.getLabel());
                }
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
