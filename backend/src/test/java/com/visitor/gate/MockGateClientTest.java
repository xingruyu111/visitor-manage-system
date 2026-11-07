package com.visitor.gate;

import com.visitor.integration.gate.GateClient;
import com.visitor.integration.gate.MockGateClient;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class MockGateClientTest {

    private final MockGateClient client = new MockGateClient();

    @Test
    void pushRevokeAndBlacklistAreRecorded() {
        assertThat(client.protocol()).isEqualTo("MOCK");
        assertThat(client.isConfigured()).isTrue();

        GateClient.GateOpResult push = client.pushPermission(new GateClient.GatePermission(
                "P1", "粤B1", "CAR", "访客", "13900000000",
                LocalDateTime.now(), LocalDateTime.now().plusHours(1)));
        assertThat(push.success()).isTrue();

        assertThat(client.revokePermission("P1").success()).isTrue();
        assertThat(client.syncBlacklist(List.of("粤B1")).success()).isTrue();
    }

    @Test
    void pullFiltersByTimeWindow() {
        LocalDateTime now = LocalDateTime.now();
        client.offer(new GateClient.GateRecord("e1", "粤B1", "IN", now, "g1"));
        client.offer(new GateClient.GateRecord("e2", "粤B2", "IN", now.minusHours(5), "g1"));

        List<GateClient.GateRecord> records = client.pullRecords(now.minusHours(1), now.plusMinutes(1));

        assertThat(records).hasSize(1);
        assertThat(records.get(0).externalId()).isEqualTo("e1");

        client.clear();
        assertThat(client.pullRecords(now.minusHours(1), now)).isEmpty();
    }
}
