package com.visitor.gate;

import com.visitor.domain.entity.VisitorPass;
import com.visitor.domain.entity.VisitorRecord;
import com.visitor.integration.IntegrationFactory;
import com.visitor.integration.gate.GateClient;
import com.visitor.server.service.impl.GateSyncService;
import com.visitor.server.service.NotificationService;
import com.visitor.server.service.AuditLogService;
import com.visitor.server.mapper.UserRoleMapper;
import com.visitor.domain.entity.UserRole;
import com.visitor.common.enums.RoleEnum;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class GateSyncServiceTest {

    private final IntegrationFactory factory = mock(IntegrationFactory.class);
    private final GateClient gate = mock(GateClient.class);
    private final NotificationService notificationService = mock(NotificationService.class);
    private final UserRoleMapper userRoleMapper = mock(UserRoleMapper.class);
    private final AuditLogService auditLogService = mock(AuditLogService.class);
    private final GateSyncService service = new GateSyncService(factory, notificationService, userRoleMapper, auditLogService);

    private final VisitorRecord record = new VisitorRecord();
    private final VisitorPass pass = new VisitorPass();

    @BeforeEach
    void setUp() {
        when(factory.gate()).thenReturn(gate);
        record.setPlateNumber("粤B12345");
        record.setVisitorName("访客");
        record.setVisitorPhone("13900000000");
        pass.setPassNo("P1");
        pass.setValidFrom(LocalDateTime.now());
        pass.setValidTo(LocalDateTime.now().plusHours(2));
    }

    @Test
    void retriesTransientFailureThenSucceeds() {
        when(gate.isConfigured()).thenReturn(true);
        when(gate.pushPermission(any()))
                .thenThrow(new RuntimeException("timeout"))
                .thenReturn(GateClient.GateOpResult.ok("ref"));

        service.push(record, pass);

        verify(gate, org.mockito.Mockito.times(2)).pushPermission(any());
        verify(notificationService, never()).notifyUser(any(), any(), any(), any(), any(), any());
    }

    @Test
    void alertsSecurityWhenAllRetriesFail() {
        when(gate.isConfigured()).thenReturn(true);
        when(gate.pushPermission(any())).thenThrow(new RuntimeException("down"));
        UserRole role = new UserRole();
        role.setUserId(9L);
        role.setRole(RoleEnum.SECURITY_ADMIN);
        when(userRoleMapper.selectList(any())).thenReturn(java.util.List.of(role));

        service.push(record, pass);

        verify(gate, org.mockito.Mockito.times(3)).pushPermission(any());
        verify(notificationService).notifyUser(eq(9L), eq("门闸权限下发失败"), any(), eq("ACCESS_ALERT"), any(), any());
        verify(auditLogService).record(eq("GATE_PUSH_FAILED"), any(), any(), any());
    }
    @Test
    void pushesPermissionWhenConfigured() {
        when(gate.isConfigured()).thenReturn(true);
        when(gate.pushPermission(any())).thenReturn(GateClient.GateOpResult.ok("ref"));

        service.push(record, pass);

        verify(gate).pushPermission(any());
    }

    @Test
    void skipsWhenNotConfiguredOrNoPlate() {
        when(gate.isConfigured()).thenReturn(false);
        service.push(record, pass);
        verify(gate, never()).pushPermission(any());

        record.setPlateNumber(null);
        service.push(record, pass);
    }

    @Test
    void failuresAreSwallowed() {
        when(gate.isConfigured()).thenReturn(true);
        when(gate.pushPermission(any())).thenThrow(new RuntimeException("gate down"));
        assertThatCode(() -> service.push(record, pass)).doesNotThrowAnyException();

        assertThatCode(() -> service.revoke("P1")).doesNotThrowAnyException();
        assertThatCode(() -> service.syncBlacklist("粤B12345")).doesNotThrowAnyException();
    }

    @Test
    void revokeAndBlacklistDelegate() {
        when(gate.isConfigured()).thenReturn(true);

        service.revoke("P1");
        service.syncBlacklist("粤b12345");

        verify(gate).revokePermission("P1");
        verify(gate).syncBlacklist(List.of("粤B12345"));
    }
}
