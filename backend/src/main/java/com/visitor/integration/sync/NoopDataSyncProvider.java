package com.visitor.integration.sync;

import com.visitor.integration.core.IntegrationNotConfiguredException;

import java.util.List;

/**
 * 未配置数据中台时的兜底实现：业务侧回落 Excel 导入与手工维护。
 */
public final class NoopDataSyncProvider implements DataSyncProvider {

    public static final NoopDataSyncProvider INSTANCE = new NoopDataSyncProvider();
    private static final String KEY = "DATA_SYNC";

    private NoopDataSyncProvider() {
    }

    @Override
    public String protocol() {
        return "NONE";
    }

    @Override
    public boolean isConfigured() {
        return false;
    }

    @Override
    public List<OrgUnit> fetchOrg() {
        throw new IntegrationNotConfiguredException(KEY);
    }

    @Override
    public List<PersonRecord> fetchStaff() {
        throw new IntegrationNotConfiguredException(KEY);
    }

    @Override
    public List<PersonRecord> fetchStudent() {
        throw new IntegrationNotConfiguredException(KEY);
    }

    @Override
    public List<PersonRecord> fetchAlumni() {
        throw new IntegrationNotConfiguredException(KEY);
    }
}
