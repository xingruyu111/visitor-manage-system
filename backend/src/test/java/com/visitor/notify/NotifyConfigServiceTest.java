package com.visitor.notify;

import com.visitor.common.exception.BizException;
import com.visitor.domain.entity.NotifyConfig;
import com.visitor.server.mapper.NotifyConfigMapper;
import com.visitor.server.service.NotifyConfigService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@SpringBootTest
@ActiveProfiles("test")
class NotifyConfigServiceTest {

    @Autowired
    private NotifyConfigService service;

    @Autowired
    private NotifyConfigMapper mapper;

    @BeforeEach
    void setUp() {
        mapper.delete(null);
    }

    @Test
    void defaultsToEnabledAndCanBeDisabled() {
        assertThat(service.isEnabled("APPROVAL_RESULT")).isTrue();

        NotifyConfig config = new NotifyConfig();
        config.setNotifyType("APPROVAL_RESULT");
        config.setEnabled(1);
        config.setChannels("SITE");
        config.setUpdatedAt(LocalDateTime.now());
        mapper.insert(config);

        assertThat(service.list()).hasSize(1);
        NotifyConfig updated = service.update("APPROVAL_RESULT", 0, "site,mp");
        assertThat(updated.getEnabled()).isZero();
        assertThat(updated.getChannels()).isEqualTo("SITE,MP");
        assertThat(service.isEnabled("APPROVAL_RESULT")).isFalse();
    }

    @Test
    void unknownTypeIsRejected() {
        assertThatThrownBy(() -> service.update("UNKNOWN", 1, null))
                .isInstanceOf(BizException.class)
                .hasMessageContaining("通知类型不存在");
    }
}
