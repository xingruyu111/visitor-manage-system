package com.visitor.server.service;

import com.visitor.domain.entity.FormField;

import java.util.List;
import java.util.Map;

public interface FormFieldService {

    List<FormField> list(String visitorType, boolean onlyActive);

    FormField create(FormField body);

    FormField update(Long id, FormField body);

    void delete(Long id);

    /** 校验指定访客类型的必填字段是否齐全。 */
    void validate(String visitorType, Map<String, String> extra);
}
