package com.visitor.integration.gate;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 门闸/停车场双向接口：下发车牌权限、撤销、同步黑名单、拉取通行记录。
 */
public interface GateClient {

    default String protocol() {
        return "REST";
    }

    boolean isConfigured();

    GateOpResult pushPermission(GatePermission permission);

    GateOpResult revokePermission(String bizNo);

    GateOpResult syncBlacklist(List<String> plateNos);

    List<GateRecord> pullRecords(LocalDateTime from, LocalDateTime to);

    record GatePermission(String bizNo, String plateNo, String plateType,
                          String visitorName, String visitorPhone,
                          LocalDateTime validFrom, LocalDateTime validTo) {
    }

    record GateOpResult(boolean success, String message, String externalRef) {

        public static GateOpResult ok(String externalRef) {
            return new GateOpResult(true, "OK", externalRef);
        }
    }

    record GateRecord(String externalId, String plateNo, String direction,
                      LocalDateTime eventTime, String gateId) {
    }
}
