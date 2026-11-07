package com.visitor.integration.sync;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * 名单同步 Mock：用于演示/联调。
 */
@Slf4j
@Component
public class MockAccessListSyncClient implements AccessListSyncPort {

    private final List<Entry> external = new CopyOnWriteArrayList<>();
    private final List<Entry> pushed = new CopyOnWriteArrayList<>();

    @Override
    public String protocol() {
        return "MOCK";
    }

    @Override
    public boolean isConfigured() {
        return true;
    }

    @Override
    public List<Entry> pull() {
        return new ArrayList<>(external);
    }

    @Override
    public void push(List<Entry> entries) {
        pushed.clear();
        pushed.addAll(entries);
        log.info("[MOCK-名单同步] 推送 {} 条", entries.size());
    }

    public void offer(Entry entry) {
        external.add(entry);
    }

    public List<Entry> pushed() {
        return pushed;
    }

    public void clear() {
        external.clear();
        pushed.clear();
    }
}
