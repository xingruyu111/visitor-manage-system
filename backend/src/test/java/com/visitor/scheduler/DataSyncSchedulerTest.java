package com.visitor.scheduler;

import com.visitor.common.enums.RoleEnum;
import com.visitor.domain.entity.UserRole;
import com.visitor.server.mapper.UserRoleMapper;
import com.visitor.server.scheduler.DataSyncScheduler;
import com.visitor.server.service.AuditLogService;
import com.visitor.server.service.NotificationService;
import com.visitor.server.service.OrgSyncService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class DataSyncSchedulerTest {

    private final OrgSyncService orgSyncService = mock(OrgSyncService.class);
    private final NotificationService notificationService = mock(NotificationService.class);
    private final UserRoleMapper userRoleMapper = mock(UserRoleMapper.class);
    private final AuditLogService auditLogService = mock(AuditLogService.class);
    private final DataSyncScheduler scheduler = new DataSyncScheduler(
            orgSyncService, notificationService, userRoleMapper, auditLogService);

    @BeforeEach
    void setUp() {
        UserRole role = new UserRole();
        role.setUserId(9L);
        role.setRole(RoleEnum.SECURITY_ADMIN);
        when(userRoleMapper.selectList(any())).thenReturn(java.util.List.of(role));
    }

    @Test
    void successfulSyncDoesNotAlert() {
        when(orgSyncService.sync(anyLong(), anyString()))
                .thenReturn(new OrgSyncService.SyncResult("org", 1, 0, 0, 0));

        int failed = scheduler.syncAll();

        org.assertj.core.api.Assertions.assertThat(failed).isZero();
        verify(notificationService, never()).notifyUser(any(), any(), any(), any(), any(), any());
    }

    @Test
    void transientFailureIsRetried() {
        when(orgSyncService.sync(anyLong(), eq("org")))
                .thenThrow(new RuntimeException("timeout"))
                .thenReturn(new OrgSyncService.SyncResult("org", 1, 0, 0, 0));
        when(orgSyncService.sync(anyLong(), eq("staff")))
                .thenReturn(new OrgSyncService.SyncResult("staff", 1, 0, 0, 0));
        when(orgSyncService.sync(anyLong(), eq("student")))
                .thenReturn(new OrgSyncService.SyncResult("student", 1, 0, 0, 0));
        when(orgSyncService.sync(anyLong(), eq("alumni")))
                .thenReturn(new OrgSyncService.SyncResult("alumni", 1, 0, 0, 0));

        int failed = scheduler.syncAll();

        org.assertj.core.api.Assertions.assertThat(failed).isZero();
        verify(orgSyncService, times(2)).sync(anyLong(), eq("org"));
        verify(notificationService, never()).notifyUser(any(), any(), any(), any(), any(), any());
    }

    @Test
    void persistentFailureAlertsAndAudits() {
        when(orgSyncService.sync(anyLong(), eq("org"))).thenThrow(new RuntimeException("down"));
        when(orgSyncService.sync(anyLong(), eq("staff")))
                .thenReturn(new OrgSyncService.SyncResult("staff", 1, 0, 0, 0));
        when(orgSyncService.sync(anyLong(), eq("student")))
                .thenReturn(new OrgSyncService.SyncResult("student", 1, 0, 0, 0));
        when(orgSyncService.sync(anyLong(), eq("alumni")))
                .thenReturn(new OrgSyncService.SyncResult("alumni", 1, 0, 0, 0));

        int failed = scheduler.syncAll();

        org.assertj.core.api.Assertions.assertThat(failed).isEqualTo(1);
        verify(orgSyncService, times(3)).sync(anyLong(), eq("org"));
        verify(notificationService).notifyUser(eq(9L), eq("数据中台同步失败"), any(), eq("SYSTEM"), eq("ORG_SYNC"), any());
        verify(auditLogService).record(eq("DATA_SYNC_FAILED"), eq("ORG_SYNC"), any(), any());
    }
}
