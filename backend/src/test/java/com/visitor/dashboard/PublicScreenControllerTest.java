package com.visitor.dashboard;

import com.visitor.server.controller.common.PublicScreenController;
import com.visitor.server.service.DashboardService;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class PublicScreenControllerTest {

    @Test
    void publicScreenReturnsDashboardData() {
        DashboardService service = mock(DashboardService.class);
        when(service.screen()).thenReturn(Map.of("summary", Map.of("total", 3)));
        PublicScreenController controller = new PublicScreenController(service);

        assertThat(controller.screen().code()).isEqualTo(200);
        assertThat(controller.screen().data()).containsKey("summary");
    }
}
