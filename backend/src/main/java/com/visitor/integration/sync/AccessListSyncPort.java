package com.visitor.integration.sync;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 名单双向同步端口：从外部系统拉取名单，或推送本地名单。
 */
public interface AccessListSyncPort {

    default String protocol() {
        return "REST";
    }

    boolean isConfigured();

    List<Entry> pull();

    void push(List<Entry> entries);

    record Entry(String targetType, String targetValue, int listType,
                 String personName, String plateNumber, LocalDateTime effectiveTo) {
    }
}
