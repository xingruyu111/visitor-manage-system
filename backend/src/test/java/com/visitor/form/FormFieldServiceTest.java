package com.visitor.form;

import com.visitor.common.exception.BizException;
import com.visitor.domain.entity.FormField;
import com.visitor.server.mapper.FormFieldMapper;
import com.visitor.server.service.FormFieldService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@SpringBootTest
@ActiveProfiles("test")
class FormFieldServiceTest {

    @Autowired
    private FormFieldService service;

    @Autowired
    private FormFieldMapper mapper;

    @BeforeEach
    void setUp() {
        mapper.delete(null);
    }

    @Test
    void crudWorks() {
        FormField created = service.create(field("company", "单位", 0));
        assertThat(service.list("NORMAL", false)).hasSize(1);

        FormField update = new FormField();
        update.setVisitorType("NORMAL");
        update.setFieldKey("company");
        update.setLabel("工作单位");
        update.setRequired(1);
        assertThat(service.update(created.getId(), update).getLabel()).isEqualTo("工作单位");

        service.delete(created.getId());
        assertThat(mapper.selectCount(null)).isZero();
    }

    @Test
    void duplicateFieldIsRejected() {
        service.create(field("car", "车牌", 0));

        assertThatThrownBy(() -> service.create(field("car", "车牌", 0)))
                .isInstanceOf(BizException.class)
                .hasMessageContaining("已存在");
    }

    @Test
    void requiredFieldsAreValidated() {
        service.create(field("company", "单位", 1));

        assertThatThrownBy(() -> service.validate("NORMAL", Map.of()))
                .hasMessageContaining("单位");
        assertThatThrownBy(() -> service.validate("NORMAL", null))
                .hasMessageContaining("单位");

        service.validate("NORMAL", Map.of("company", "某某公司"));
        service.validate("ALUMNI", Map.of());
    }

    @Test
    void invalidBodyIsRejected() {
        assertThatThrownBy(() -> service.create(new FormField()))
                .hasMessageContaining("访客类型");
        assertThatThrownBy(() -> service.update(999L, field("x", "x", 0)))
                .hasMessageContaining("字段不存在");
        assertThatThrownBy(() -> service.delete(999L))
                .hasMessageContaining("字段不存在");
    }

    private FormField field(String key, String label, int required) {
        FormField field = new FormField();
        field.setVisitorType("NORMAL");
        field.setFieldKey(key);
        field.setLabel(label);
        field.setFieldType("TEXT");
        field.setRequired(required);
        return field;
    }
}
