package com.visitor.notify;

import com.visitor.domain.entity.MpConfig;
import com.visitor.domain.entity.User;
import com.visitor.server.mapper.MpConfigMapper;
import com.visitor.server.mapper.UserMapper;
import com.visitor.server.service.MpConfigService;
import com.visitor.server.service.impl.WxMpNotifier;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;

@SpringBootTest
@ActiveProfiles("test")
class MpNotifyTest {

    @Autowired
    private MpConfigMapper mpConfigMapper;

    @Autowired
    private MpConfigService mpConfigService;

    @Autowired
    private WxMpNotifier notifier;

    @Autowired
    private UserMapper userMapper;

    @BeforeEach
    void setUp() {
        mpConfigMapper.delete(null);
    }

    @Test
    void skipsWhenNotConfigured() {
        assertThatCode(() -> notifier.sendAuditResult(1L, "访客", "通过")).doesNotThrowAnyException();
    }

    @Test
    void skipsWhenUserHasNoOpenid() {
        insertConfig();
        Long userId = createUser(null);

        assertThatCode(() -> notifier.sendAuditResult(userId, "访客", "通过")).doesNotThrowAnyException();
    }

    @Test
    void sendFailureIsSwallowed() {
        insertConfig();
        Long userId = createUser("openid-1");

        assertThatCode(() -> notifier.sendAuditResult(userId, "访客", "通过")).doesNotThrowAnyException();
    }

    @Test
    void configCanBeRead() {
        insertConfig();

        assertThat(mpConfigService.get().getAppId()).isEqualTo("wx-test");
        assertThat(mpConfigService.get().getAuditResultTemplateId()).isEqualTo("tpl-1");
    }

    private void insertConfig() {
        MpConfig config = new MpConfig();
        config.setAppId("wx-test");
        config.setSecret("secret");
        config.setAuditResultTemplateId("tpl-1");
        config.setCreatedAt(LocalDateTime.now());
        config.setUpdatedAt(LocalDateTime.now());
        mpConfigMapper.insert(config);
    }

    private Long createUser(String openid) {
        User user = new User();
        user.setPhone("1390000900" + (openid == null ? "1" : "2"));
        user.setName("通知用户");
        user.setPassword("x");
        user.setMpOpenid(openid);
        user.setStatus(1);
        user.setCreatedAt(LocalDateTime.now());
        user.setUpdatedAt(LocalDateTime.now());
        userMapper.insert(user);
        return user.getId();
    }
}
