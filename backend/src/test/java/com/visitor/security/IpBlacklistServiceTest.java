package com.visitor.security;

import com.visitor.common.exception.BizException;
import com.visitor.domain.entity.IpBlacklist;
import com.visitor.server.mapper.IpBlacklistMapper;
import com.visitor.server.service.IpBlacklistService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@SpringBootTest
@ActiveProfiles("test")
class IpBlacklistServiceTest {

    @Autowired
    private IpBlacklistService service;

    @Autowired
    private IpBlacklistMapper mapper;

    @BeforeEach
    void setUp() {
        mapper.delete(null);
    }

    @Test
    void permanentAndExpiredEntries() {
        IpBlacklist permanent = service.add("1.2.3.4", "攻击", null, 1L);
        assertThatThrownBy(() -> service.assertAllowed("1.2.3.4"))
                .isInstanceOf(BizException.class)
                .hasMessageContaining("禁止访问");

        IpBlacklist expired = service.add("5.6.7.8", "临时", LocalDateTime.now().minusMinutes(1), 1L);
        assertThatCode(() -> service.assertAllowed("5.6.7.8")).doesNotThrowAnyException();

        assertThat(service.page("1.2", 1, 10).total()).isEqualTo(1);
        service.remove(permanent.getId());
        assertThat(mapper.selectById(permanent.getId())).isNull();
        service.remove(expired.getId());
        assertThatThrownBy(() -> service.remove(999L)).hasMessageContaining("不存在");
    }

    @Test
    void duplicateAndBlankAreRejected() {
        service.add("9.9.9.9", null, null, 1L);

        assertThatThrownBy(() -> service.add("9.9.9.9", null, null, 1L)).hasMessageContaining("已在黑名单");
        assertThatThrownBy(() -> service.add(" ", null, null, 1L)).hasMessageContaining("IP 不能为空");
        assertThatCode(() -> service.assertAllowed(null)).doesNotThrowAnyException();
        assertThatCode(() -> service.assertAllowed("8.8.8.8")).doesNotThrowAnyException();
    }
}
