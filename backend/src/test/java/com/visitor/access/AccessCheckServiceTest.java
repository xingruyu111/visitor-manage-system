package com.visitor.access;

import com.visitor.common.exception.BizException;
import com.visitor.domain.entity.AccessList;
import com.visitor.server.mapper.AccessListMapper;
import com.visitor.server.service.AccessCheckService;
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
class AccessCheckServiceTest {

    @Autowired
    private AccessCheckService service;

    @Autowired
    private AccessListMapper mapper;

    @BeforeEach
    void setUp() {
        mapper.delete(null);
    }

    @Test
    void blacklistAndWhitelistAreDetected() {
        AccessList black = new AccessList();
        black.setListType(1);
        black.setTargetType("PERSON");
        black.setTargetValue("13800009991");
        service.create(1L, black);

        AccessList white = new AccessList();
        white.setListType(2);
        white.setTargetType("PLATE_MOTOR");
        white.setTargetValue("粤b12345");
        service.create(1L, white);

        assertThat(service.check(1L, "13800009991", null, null)).isEqualTo("BLACKLIST");
        assertThat(service.check(1L, null, null, "粤B12345")).isEqualTo("WHITELIST");
        assertThat(service.check(1L, "other", null, null)).isEqualTo("NONE");
        assertThat(service.check(1L, null, null, null)).isEqualTo("NONE");
    }

    @Test
    void duplicateEntryIsRejected() {
        AccessList black = new AccessList();
        black.setListType(1);
        black.setTargetType("PERSON");
        black.setTargetValue("13800009992");
        service.create(1L, black);

        assertThatThrownBy(() -> service.create(1L, black)).hasMessageContaining("已在");
    }

    @Test
    void updateAndDeleteWork() {
        AccessList black = new AccessList();
        black.setListType(1);
        black.setTargetType("PERSON");
        black.setTargetValue("13800009993");
        AccessList created = service.create(1L, black);

        AccessList update = new AccessList();
        update.setListType(1);
        update.setTargetType("PERSON");
        update.setTargetValue("13800009993");
        update.setReason("改期");
        update.setStatus(0);
        AccessList saved = service.update(1L, created.getId(), update);
        assertThat(saved.getReason()).isEqualTo("改期");
        assertThat(saved.getStatus()).isZero();

        service.delete(1L, created.getId());
        assertThat(mapper.selectById(created.getId())).isNull();
    }

    @Test
    void invalidInputsAreRejected() {
        AccessList bad = new AccessList();
        bad.setListType(9);
        assertThatThrownBy(() -> service.create(1L, bad)).hasMessageContaining("名单类型");

        AccessList noTarget = new AccessList();
        noTarget.setListType(1);
        noTarget.setTargetType("PERSON");
        assertThatThrownBy(() -> service.create(1L, noTarget)).hasMessageContaining("对象值");

        AccessList badWindow = new AccessList();
        badWindow.setListType(1);
        badWindow.setTargetType("PERSON");
        badWindow.setTargetValue("13800009994");
        badWindow.setEffectiveFrom(LocalDateTime.now());
        badWindow.setEffectiveTo(LocalDateTime.now().minusDays(1));
        assertThatThrownBy(() -> service.create(1L, badWindow)).hasMessageContaining("失效时间");
    }

    @Test
    void expiredEntriesStopMatchingAndExpireOverdue() {
        AccessList expired = new AccessList();
        expired.setListType(1);
        expired.setTargetType("PERSON");
        expired.setTargetValue("13800009995");
        expired.setEffectiveTo(LocalDateTime.now().minusHours(1));
        AccessList created = service.create(1L, expired);
        created.setStatus(1);
        mapper.updateById(created);

        assertThat(service.check(1L, "13800009995", null, null)).isEqualTo("NONE");
        assertThat(service.expireOverdue()).isGreaterThanOrEqualTo(1);
        assertThat(mapper.selectById(created.getId()).getStatus()).isZero();
    }

    @Test
    void pageFiltersByTypeAndKeyword() {
        for (int i = 1; i <= 2; i++) {
            AccessList white = new AccessList();
            white.setListType(2);
            white.setTargetType("PERSON");
            white.setTargetValue("1380000999" + i);
            white.setPersonName("免预约" + i);
            service.create(1L, white);
        }

        assertThat(service.page(1L, 2, "PERSON", "免预约", 1, 10).total()).isEqualTo(2);
        assertThat(service.page(1L, 1, null, null, 1, 10).total()).isZero();
        assertThat(service.page(2L, null, null, null, 1, 10).total()).isZero();
    }
}
