package com.visitor.integration.sync;

import java.util.List;

/**
 * 数据中台同步：组织、教职工、学生、校友。
 */
public interface DataSyncProvider {

    boolean isConfigured();

    SyncResult syncOrg();

    SyncResult syncStaff();

    SyncResult syncStudent();

    SyncResult syncAlumni();

    record SyncResult(String type, int total, int created, int updated, int failed,
                      List<String> errors, String traceId) {

        public static SyncResult empty(String type) {
            return new SyncResult(type, 0, 0, 0, 0, List.of(), null);
        }
    }
}
