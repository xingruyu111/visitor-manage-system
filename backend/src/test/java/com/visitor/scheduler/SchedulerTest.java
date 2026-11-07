package com.visitor.scheduler;

import com.visitor.server.scheduler.VisitorExpireScheduler;
import com.visitor.server.scheduler.OverstayScheduler;
import com.visitor.server.scheduler.GateRecordScheduler;
import com.visitor.server.service.GateRecordIntakeService;
import com.visitor.server.service.AlarmService;
import com.visitor.server.service.VisitorService;
import com.visitor.workflow.ApprovalEngine;
import com.visitor.workflow.ApprovalTimeoutScheduler;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class SchedulerTest {

    private final ApprovalEngine approvalEngine = mock(ApprovalEngine.class);
    private final VisitorService visitorService = mock(VisitorService.class);

    private final ApprovalTimeoutScheduler approvalScheduler = new ApprovalTimeoutScheduler(approvalEngine);
    private final VisitorExpireScheduler visitorScheduler = new VisitorExpireScheduler(visitorService);
    private final AlarmService alarmService = mock(AlarmService.class);
    private final OverstayScheduler overstayScheduler = new OverstayScheduler(alarmService);
    private final GateRecordIntakeService gateRecordIntakeService = mock(GateRecordIntakeService.class);
    private final GateRecordScheduler gateRecordScheduler = new GateRecordScheduler(gateRecordIntakeService);

    @Test
    void gateRecordSchedulerDelegatesAndSwallows() {
        when(gateRecordIntakeService.pull()).thenReturn(2);
        gateRecordScheduler.pull();
        verify(gateRecordIntakeService).pull();

        when(gateRecordIntakeService.pull()).thenThrow(new RuntimeException("boom"));
        assertThatCode(gateRecordScheduler::pull).doesNotThrowAnyException();
    }
    @Test
    void approvalSchedulerDelegatesToEngine() {
        when(approvalEngine.expireOverdueTasks()).thenReturn(2);

        approvalScheduler.expireOverdueTasks();

        verify(approvalEngine).expireOverdueTasks();
    }

    @Test
    void approvalSchedulerSwallowsExceptions() {
        when(approvalEngine.expireOverdueTasks()).thenThrow(new RuntimeException("boom"));

        assertThatCode(approvalScheduler::expireOverdueTasks).doesNotThrowAnyException();
    }

    @Test
    void visitorSchedulerDelegatesToService() {
        when(visitorService.expireOverdue()).thenReturn(1);

        visitorScheduler.expireOverdueRecords();

        verify(visitorService).expireOverdue();
    }

    @Test
    void visitorSchedulerSwallowsExceptions() {
        when(visitorService.expireOverdue()).thenThrow(new RuntimeException("boom"));

        assertThatCode(visitorScheduler::expireOverdueRecords).doesNotThrowAnyException();
    }
}
