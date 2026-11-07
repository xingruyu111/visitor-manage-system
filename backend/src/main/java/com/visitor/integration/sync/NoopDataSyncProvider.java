package com.visitor.integration.sync;

import com.visitor.integration.core.IntegrationNotConfiguredException;

/**
 * 未配置数据中台时的兜底实现：业务侧回落 Excel 导入与手工维护。
 */
public final class NoopDataSyncProvider implements DataSyncProvider {

    public static final NoopDataSyncProvider INSTANCE = new NoopDataSyncProvider();
    private static final String KEY = "DATA_SYNC";

    private NoopDataSyncProvider() {
    }

    @Override
    public boolean isConfigured() {
        return false;
    }

    @Override
    public SyncResult syncOrg() {
        throw new IntegrationNotConfiguredException(KEY);
    }

    @Override
    public SyncResult syncStaff() {
        throw new IntegrationNotConfiguredException(KEY);
    }

    @Override
    public SyncResult syncStudent() {
        throw new IntegrationNotConfiguredException(KEY);
    }

    @Override
    public SyncResult syncAlumni() {
        throw new IntegrationNotConfiguredException(KEY);
    }
}
