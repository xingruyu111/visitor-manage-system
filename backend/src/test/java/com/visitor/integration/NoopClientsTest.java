package com.visitor.integration;

import com.visitor.integration.core.IntegrationNotConfiguredException;
import com.visitor.integration.face.NoopFaceClient;
import com.visitor.integration.gate.NoopGateClient;
import com.visitor.integration.portal.NoopPortalClient;
import com.visitor.integration.sso.NoopIdentityProvider;
import com.visitor.integration.sync.NoopDataSyncProvider;
import com.visitor.integration.todo.NoopTodoMessageClient;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class NoopClientsTest {

    @Test
    void allNoopsReportNotConfigured() {
        assertThat(NoopIdentityProvider.INSTANCE.isConfigured()).isFalse();
        assertThat(NoopDataSyncProvider.INSTANCE.isConfigured()).isFalse();
        assertThat(NoopGateClient.INSTANCE.isConfigured()).isFalse();
        assertThat(NoopFaceClient.INSTANCE.isConfigured()).isFalse();
        assertThat(NoopPortalClient.INSTANCE.isConfigured()).isFalse();
        assertThat(NoopTodoMessageClient.INSTANCE.isConfigured()).isFalse();
    }

    @Test
    void ssoThrowsWhenUsed() {
        assertThatThrownBy(() -> NoopIdentityProvider.INSTANCE.buildAuthorizeUrl("r", "s"))
                .isInstanceOf(IntegrationNotConfiguredException.class)
                .hasMessageContaining("SSO");
        assertThatThrownBy(() -> NoopIdentityProvider.INSTANCE.exchange("code", "r"))
                .isInstanceOf(IntegrationNotConfiguredException.class);
    }

    @Test
    void dataSyncThrowsWhenUsed() {
        assertThatThrownBy(() -> NoopDataSyncProvider.INSTANCE.syncOrg())
                .isInstanceOf(IntegrationNotConfiguredException.class);
        assertThatThrownBy(() -> NoopDataSyncProvider.INSTANCE.syncStaff())
                .isInstanceOf(IntegrationNotConfiguredException.class);
        assertThatThrownBy(() -> NoopDataSyncProvider.INSTANCE.syncStudent())
                .isInstanceOf(IntegrationNotConfiguredException.class);
        assertThatThrownBy(() -> NoopDataSyncProvider.INSTANCE.syncAlumni())
                .isInstanceOf(IntegrationNotConfiguredException.class);
    }

    @Test
    void gateThrowsWhenUsed() {
        assertThatThrownBy(() -> NoopGateClient.INSTANCE.pushPermission(null))
                .isInstanceOf(IntegrationNotConfiguredException.class);
        assertThatThrownBy(() -> NoopGateClient.INSTANCE.revokePermission("biz"))
                .isInstanceOf(IntegrationNotConfiguredException.class);
        assertThatThrownBy(() -> NoopGateClient.INSTANCE.syncBlacklist(List.of("京A12345")))
                .isInstanceOf(IntegrationNotConfiguredException.class);
        assertThatThrownBy(() -> NoopGateClient.INSTANCE.pullRecords(LocalDateTime.now(), LocalDateTime.now()))
                .isInstanceOf(IntegrationNotConfiguredException.class);
    }

    @Test
    void faceThrowsWhenUsed() {
        assertThatThrownBy(() -> NoopFaceClient.INSTANCE.enroll(null))
                .isInstanceOf(IntegrationNotConfiguredException.class);
        assertThatThrownBy(() -> NoopFaceClient.INSTANCE.revoke("p1"))
                .isInstanceOf(IntegrationNotConfiguredException.class);
        assertThatThrownBy(() -> NoopFaceClient.INSTANCE.verify("p1", new byte[0]))
                .isInstanceOf(IntegrationNotConfiguredException.class);
    }

    @Test
    void portalThrowsWhenUsed() {
        assertThatThrownBy(() -> NoopPortalClient.INSTANCE.buildSsoUrl("/x"))
                .isInstanceOf(IntegrationNotConfiguredException.class);
        assertThatThrownBy(() -> NoopPortalClient.INSTANCE.validateTicket("t"))
                .isInstanceOf(IntegrationNotConfiguredException.class);
    }

    @Test
    void todoThrowsWhenUsed() {
        assertThatThrownBy(() -> NoopTodoMessageClient.INSTANCE.pushTodo(null))
                .isInstanceOf(IntegrationNotConfiguredException.class);
        assertThatThrownBy(() -> NoopTodoMessageClient.INSTANCE.pushMessage(null))
                .isInstanceOf(IntegrationNotConfiguredException.class);
    }
}
