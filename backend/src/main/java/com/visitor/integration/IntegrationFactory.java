package com.visitor.integration;

import com.visitor.domain.entity.IntegrationConfig;
import com.visitor.integration.config.IntegrationConfigService;
import com.visitor.integration.face.FaceClient;
import com.visitor.integration.face.NoopFaceClient;
import com.visitor.integration.gate.GateClient;
import com.visitor.integration.gate.NoopGateClient;
import com.visitor.integration.portal.NoopPortalClient;
import com.visitor.integration.portal.PortalClient;
import com.visitor.integration.sso.IdentityProvider;
import com.visitor.integration.sso.NoopIdentityProvider;
import com.visitor.integration.sync.DataSyncProvider;
import com.visitor.integration.sync.NoopDataSyncProvider;
import com.visitor.integration.todo.NoopTodoMessageClient;
import com.visitor.integration.todo.TodoMessageClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.function.Function;

/**
 * 根据配置选择实现：未启用或协议不匹配时一律返回 Noop。
 *
 * <p>新增一所学校的实现只需实现对应 Port 并注册为 Bean，无需改这里。</p>
 */
@Component
public class IntegrationFactory {

    private static final Logger log = LoggerFactory.getLogger(IntegrationFactory.class);

    private final IntegrationConfigService configService;
    private final List<IdentityProvider> identityProviders;
    private final List<DataSyncProvider> dataSyncProviders;
    private final List<GateClient> gateClients;
    private final List<FaceClient> faceClients;
    private final List<PortalClient> portalClients;
    private final List<TodoMessageClient> todoMessageClients;

    public IntegrationFactory(IntegrationConfigService configService,
                              List<IdentityProvider> identityProviders,
                              List<DataSyncProvider> dataSyncProviders,
                              List<GateClient> gateClients,
                              List<FaceClient> faceClients,
                              List<PortalClient> portalClients,
                              List<TodoMessageClient> todoMessageClients) {
        this.configService = configService;
        this.identityProviders = identityProviders;
        this.dataSyncProviders = dataSyncProviders;
        this.gateClients = gateClients;
        this.faceClients = faceClients;
        this.portalClients = portalClients;
        this.todoMessageClients = todoMessageClients;
    }

    public IdentityProvider identity() {
        return pick(IntegrationKey.SSO, identityProviders, IdentityProvider::protocol, NoopIdentityProvider.INSTANCE);
    }

    public DataSyncProvider dataSync() {
        return pick(IntegrationKey.DATA_SYNC, dataSyncProviders, DataSyncProvider::protocol, NoopDataSyncProvider.INSTANCE);
    }

    public GateClient gate() {
        return pick(IntegrationKey.GATE, gateClients, client -> "REST", NoopGateClient.INSTANCE);
    }

    public FaceClient face() {
        return pick(IntegrationKey.FACE, faceClients, client -> "REST", NoopFaceClient.INSTANCE);
    }

    public PortalClient portal() {
        return pick(IntegrationKey.PORTAL, portalClients, client -> "REST", NoopPortalClient.INSTANCE);
    }

    public TodoMessageClient todoMessage() {
        return pick(IntegrationKey.TODO_MSG, todoMessageClients, TodoMessageClient::protocol, NoopTodoMessageClient.INSTANCE);
    }

    private <T> T pick(IntegrationKey key, List<T> implementations,
                       Function<T, String> protocolOf, T fallback) {
        IntegrationConfig config = configService.getEnabled(key);
        if (config == null || config.getProtocol() == null || config.getProtocol().isBlank()) {
            return fallback;
        }
        return implementations.stream()
                .filter(item -> config.getProtocol().equalsIgnoreCase(protocolOf.apply(item)))
                .findFirst()
                .orElseGet(() -> {
                    log.warn("集成项 {} 配置的协议 {} 暂无实现，降级为 Noop", key, config.getProtocol());
                    return fallback;
                });
    }
}
