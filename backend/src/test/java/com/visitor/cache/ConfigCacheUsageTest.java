package com.visitor.cache;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.visitor.domain.dto.BookingRule;
import com.visitor.domain.entity.FormField;
import com.visitor.domain.entity.SystemSetting;
import com.visitor.domain.entity.VisitConfig;
import com.visitor.domain.entity.VisitorType;
import com.visitor.server.cache.ConfigCacheService;
import com.visitor.server.mapper.FormFieldMapper;
import com.visitor.server.mapper.SystemSettingMapper;
import com.visitor.server.mapper.VisitConfigMapper;
import com.visitor.server.mapper.VisitorTypeMapper;
import com.visitor.server.service.impl.FormFieldServiceImpl;
import com.visitor.server.service.impl.SystemSettingServiceImpl;
import com.visitor.server.service.impl.VisitConfigServiceImpl;
import com.visitor.server.service.impl.VisitorTypeServiceImpl;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class ConfigCacheUsageTest {

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Test
    void visitorTypeListAndRulesUseCache() {
        VisitorTypeMapper mapper = mock(VisitorTypeMapper.class);
        ConfigCacheService cache = mock(ConfigCacheService.class);
        VisitorType type = new VisitorType();
        type.setCode("NORMAL");
        type.setName("普通访客");
        when(cache.getList("visitor-types:all", VisitorType.class)).thenReturn(null, List.of(type));
        when(mapper.selectList(any())).thenReturn(List.of(type));
        VisitorTypeServiceImpl service = new VisitorTypeServiceImpl(
                mapper, objectMapper, mock(com.visitor.server.mapper.FormFieldMapper.class), cache);

        assertThat(service.list(false)).hasSize(1);
        assertThat(service.list(false)).hasSize(1);
        verify(mapper, times(1)).selectList(any());
        verify(cache).put("visitor-types:all", List.of(type));

        BookingRule rule = new BookingRule();
        rule.setMode("UNLIMITED");
        when(cache.getList("visitor-type:rules:NORMAL", BookingRule.class)).thenReturn(List.of(rule));
        assertThat(service.rulesOf("normal")).hasSize(1);
    }

    @Test
    void visitorTypeMutationEvictsCache() {
        VisitorTypeMapper mapper = mock(VisitorTypeMapper.class);
        ConfigCacheService cache = mock(ConfigCacheService.class);
        VisitorType type = new VisitorType();
        type.setId(1L);
        type.setCode("NORMAL");
        type.setStatus(1);
        when(mapper.selectById(1L)).thenReturn(type);
        VisitorTypeServiceImpl service = new VisitorTypeServiceImpl(
                mapper, objectMapper, mock(com.visitor.server.mapper.FormFieldMapper.class), cache);

        service.updateStatus(1L, 0);

        verify(cache).evict(any(String[].class));
    }

    @Test
    void formFieldListUsesCacheAndMutationEvicts() {
        FormFieldMapper mapper = mock(FormFieldMapper.class);
        ConfigCacheService cache = mock(ConfigCacheService.class);
        FormField field = new FormField();
        field.setVisitorType("NORMAL");
        when(cache.getList(any(), eq(FormField.class))).thenReturn(null);
        when(mapper.selectList(any())).thenReturn(List.of(field));
        FormFieldServiceImpl service = new FormFieldServiceImpl(mapper, cache);

        assertThat(service.list("NORMAL", true)).hasSize(1);
        verify(cache).put("form-fields:NORMAL:true", List.of(field));

        when(mapper.selectById(1L)).thenReturn(field);
        service.delete(1L);
        verify(cache).evict("form-fields:ALL:true", "form-fields:ALL:false",
                "form-fields:NORMAL:true", "form-fields:NORMAL:false");
    }

    @Test
    void visitConfigAndSettingUseCache() {
        VisitConfigMapper visitMapper = mock(VisitConfigMapper.class);
        ConfigCacheService cache = mock(ConfigCacheService.class);
        VisitConfig config = new VisitConfig();
        config.setSchoolId(1L);
        config.setAlumniMonthsJson("[1,2]");
        when(cache.getList(any(), eq(Integer.class))).thenReturn(null);
        when(visitMapper.selectOne(any())).thenReturn(config);
        VisitConfigServiceImpl visitService = new VisitConfigServiceImpl(visitMapper, objectMapper, cache);
        assertThat(visitService.alumniMonths()).containsExactly(1, 2);
        verify(cache).put("visit-config:alumni-months", List.of(1, 2));

        SystemSettingMapper settingMapper = mock(SystemSettingMapper.class);
        SystemSetting setting = new SystemSetting();
        setting.setSettingKey("visit.max_stay_hours");
        setting.setSettingValue("12");
        when(settingMapper.selectOne(any())).thenReturn(setting);
        SystemSettingServiceImpl settingService = new SystemSettingServiceImpl(settingMapper, cache);
        assertThat(settingService.get("visit.max_stay_hours", "8")).isEqualTo("12");
        verify(cache).put("setting:visit.max_stay_hours", "12");
    }
}
