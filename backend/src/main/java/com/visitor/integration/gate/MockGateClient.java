package com.visitor.integration.gate;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * 门闸 Mock 实现：用于演示/联调下发与记录回传。
 * 集成配置 GATE 的 protocol 设为 MOCK 即可启用。
 */
@Slf4j
@Component
public class MockGateClient implements GateClient {

    private final List<GateRecord> records = new CopyOnWriteArrayList<>();
    private final List<GatePermission> permissions = new CopyOnWriteArrayList<>();
    private final List<String> revoked = new CopyOnWriteArrayList<>();

    @Override
    public String protocol() {
        return "MOCK";
    }

    @Override
    public boolean isConfigured() {
        return true;
    }

    @Override
    public GateOpResult pushPermission(GatePermission permission) {
        permissions.add(permission);
        log.info("[MOCK-门闸] 下发权限 pass={}, plate={}", permission.bizNo(), permission.plateNo());
        return GateOpResult.ok("mock-" + permission.bizNo());
    }

    @Override
    public GateOpResult revokePermission(String bizNo) {
        revoked.add(bizNo);
        return GateOpResult.ok("mock-revoke-" + bizNo);
    }

    @Override
    public GateOpResult syncBlacklist(List<String> plateNos) {
        return GateOpResult.ok("mock-blacklist");
    }

    @Override
    public List<GateRecord> pullRecords(LocalDateTime from, LocalDateTime to) {
        List<GateRecord> result = new ArrayList<>();
        for (GateRecord record : records) {
            if (!record.eventTime().isBefore(from) && !record.eventTime().isAfter(to)) {
                result.add(record);
            }
        }
        return result;
    }

    /** 测试/演示：模拟门闸产生一条记录。 */
    public void offer(GateRecord record) {
        records.add(record);
    }

    public void clear() {
        records.clear();
        permissions.clear();
        revoked.clear();
    }
}
