package com.visitor.setting;

import com.visitor.common.exception.BizException;
import com.visitor.domain.entity.SystemSetting;
import com.visitor.server.mapper.SystemSettingMapper;
import com.visitor.server.service.SystemSettingService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@SpringBootTest
@ActiveProfiles("test")
class SystemSettingServiceTest {

    @Autowired
    private SystemSettingService service;

    @Autowired
    private SystemSettingMapper mapper;

    @BeforeEach
    void setUp() {
        mapper.delete(null);
    }

    @Test
    void readsTypedValuesWithDefaults() {
        insert("visit.pass_modes", "WALK, EBIKE");
        insert("visit.max_stay_hours", "8");
        insert("visit.duplicate_check_enabled", "false");

        assertThat(service.getList("visit.pass_modes", "WALK")).containsExactly("WALK", "EBIKE");
        assertThat(service.getInt("visit.max_stay_hours", 12)).isEqualTo(8);
        assertThat(service.getBool("visit.duplicate_check_enabled", true)).isFalse();
        assertThat(service.get("missing.key", "fallback")).isEqualTo("fallback");
        assertThat(service.getInt("missing.int", 5)).isEqualTo(5);
        assertThat(service.getBool("missing.bool", true)).isTrue();
        assertThat(service.list()).hasSize(3);
    }

    @Test
    void updatesAndValidatesKeys() {
        SystemSetting setting = insert("visit.max_stay_hours", "12");

        assertThat(service.update("visit.max_stay_hours", "24").getSettingValue()).isEqualTo("24");
        assertThat(service.getInt("visit.max_stay_hours", 12)).isEqualTo(24);

        assertThatThrownBy(() -> service.update("unknown.key", "x"))
                .isInstanceOf(BizException.class)
                .hasMessageContaining("参数不存在");
        assertThat(setting.getId()).isNotNull();
    }

    @org.junit.jupiter.api.AfterEach
    void tearDown() {
        mapper.delete(null);
    }

    private SystemSetting insert(String key, String value) {
        SystemSetting setting = new SystemSetting();
        setting.setSettingKey(key);
        setting.setSettingValue(value);
        setting.setUpdatedAt(LocalDateTime.now());
        mapper.insert(setting);
        return setting;
    }
}
