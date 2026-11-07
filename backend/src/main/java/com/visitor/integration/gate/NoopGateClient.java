package com.visitor.integration.gate;

import com.visitor.integration.core.IntegrationNotConfiguredException;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 未配置门闸时的兜底实现：仅保留本地扫码核销与通行流水。
 */
public final class NoopGateClient implements GateClient {

    public static final NoopGateClient INSTANCE = new NoopGateClient();
    private static final String KEY = "GATE";

    private NoopGateClient() {
    }

    @Override
    public boolean isConfigured() {
        return false;
    }

    @Override
    public GateOpResult pushPermission(GatePermission permission) {
        throw new IntegrationNotConfiguredException(KEY);
    }

    @Override
    public GateOpResult revokePermission(String bizNo) {
        throw new IntegrationNotConfiguredException(KEY);
    }

    @Override
    public GateOpResult syncBlacklist(List<String> plateNos) {
        throw new IntegrationNotConfiguredException(KEY);
    }

    @Override
    public List<GateRecord> pullRecords(LocalDateTime from, LocalDateTime to) {
        throw new IntegrationNotConfiguredException(KEY);
    }
}
