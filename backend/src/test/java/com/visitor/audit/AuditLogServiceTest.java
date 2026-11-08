package com.visitor.audit;

import com.visitor.common.enums.RoleEnum;
import com.visitor.domain.entity.AuditLog;
import com.visitor.security.UserContext;
import com.visitor.server.mapper.AuditLogMapper;
import com.visitor.server.service.AuditLogService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@ActiveProfiles("test")
class AuditLogServiceTest {

    @Autowired
    private AuditLogService service;

    @Autowired
    private AuditLogMapper mapper;

    @BeforeEach
    void setUp() {
        mapper.delete(null);
        UserContext.set(7L, "审计用户", RoleEnum.SECURITY_ADMIN, 1L);
    }

    @AfterEach
    void tearDown() {
        UserContext.clear();
        RequestContextHolder.resetRequestAttributes();
    }

    @Test
    void recordsOperatorAndSupportsQuery() {
        service.record("VISITOR_APPLY", "VISITOR_RECORD", 1L, "提交预约：张三 → 李老师");

        AuditLog log = mapper.selectList(null).get(0);
        assertThat(log.getUserId()).isEqualTo(7L);
        assertThat(log.getUserName()).isEqualTo("审计用户");
        assertThat(log.getAction()).isEqualTo("VISITOR_APPLY");

        assertThat(service.page("VISITOR_APPLY", "VISITOR_RECORD", null, 1, 10).total()).isEqualTo(1);
        assertThat(service.page("APPROVAL_APPROVE", null, null, 1, 10).total()).isZero();
        assertThat(service.page(null, null, "审计用户", 1, 10).total()).isEqualTo(1);
        assertThat(service.exportRows(null, null, "张三")).hasSize(1);
    }

    @Test
    void capturesForwardedIpAndRemoteAddr() {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.addHeader("X-Forwarded-For", "1.2.3.4, 5.6.7.8");
        RequestContextHolder.setRequestAttributes(new ServletRequestAttributes(request));
        service.record("ACCESS_VERIFY", "VISITOR_RECORD", 2L, "核验：IN");
        assertThat(mapper.selectList(null).get(0).getIp()).isEqualTo("1.2.3.4");

        mapper.delete(null);
        MockHttpServletRequest direct = new MockHttpServletRequest();
        direct.setRemoteAddr("10.0.0.8");
        RequestContextHolder.setRequestAttributes(new ServletRequestAttributes(direct));
        service.record("ACCESS_VERIFY", "VISITOR_RECORD", 3L, "核验：OUT");
        assertThat(mapper.selectList(null).get(0).getIp()).isEqualTo("10.0.0.8");
    }

    @Test
    void longDetailIsTruncated() {
        service.record("TEST", "SYSTEM", 1L, "x".repeat(600));

        assertThat(mapper.selectList(null).get(0).getDetail()).hasSize(500);
    }
}
