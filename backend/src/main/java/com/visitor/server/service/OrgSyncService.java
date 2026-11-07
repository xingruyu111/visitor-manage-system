package com.visitor.server.service;

public interface OrgSyncService {

    SyncResult sync(Long schoolId, String type);

    record SyncResult(String type, int created, int updated, int deactivated, int failed) {
    }
}
