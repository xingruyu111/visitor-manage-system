package com.visitor.dept;

import com.alibaba.excel.EasyExcel;
import com.visitor.common.core.Result;
import com.visitor.common.enums.RoleEnum;
import com.visitor.common.exception.BizException;
import com.visitor.domain.dto.VisitorBatchDTO;
import com.visitor.domain.dto.VisitorBatchImportRow;
import com.visitor.security.UserContext;
import com.visitor.server.controller.dept.DeptVisitorRecordController;
import com.visitor.server.service.VisitorService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockMultipartFile;

import java.io.ByteArrayOutputStream;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class DeptBatchImportTest {

    @AfterEach
    void clearContext() {
        UserContext.clear();
    }

    @Test
    void importsExcelRowsAndReportsSuccess() {
        VisitorService service = mock(VisitorService.class);
        when(service.deptBatch(eq(1L), any(VisitorBatchDTO.class), eq(9L))).thenReturn(1);
        DeptVisitorRecordController controller = new DeptVisitorRecordController(service);
        UserContext.set(9L, "经办人", RoleEnum.DEPT_OPERATOR, 1L);

        Result<Map<String, Object>> result = controller.importBatch(file(row("张三", "2026-11-12 08:00:00")));

        assertThat(result.data().get("success")).isEqualTo(1);
        assertThat(result.data().get("failed")).isEqualTo(0);
    }

    @Test
    void reportsFailedRowsWithExcelRowNumber() {
        VisitorService service = mock(VisitorService.class);
        when(service.deptBatch(eq(1L), any(VisitorBatchDTO.class), eq(9L)))
                .thenThrow(new BizException(400, "时间不合法"));
        DeptVisitorRecordController controller = new DeptVisitorRecordController(service);
        UserContext.set(9L, "经办人", RoleEnum.DEPT_OPERATOR, 1L);

        Result<Map<String, Object>> result = controller.importBatch(file(row("李四", "2026-11-12 09:00:00")));

        assertThat(result.data().get("success")).isEqualTo(0);
        assertThat(result.data().get("failed")).isEqualTo(1);
        List<?> errors = (List<?>) result.data().get("errors");
        assertThat(((Map<?, ?>) errors.get(0)).get("row")).isEqualTo(2);
        assertThat(((Map<?, ?>) errors.get(0)).get("reason")).isEqualTo("时间不合法");
    }

    private VisitorBatchImportRow row(String name, String time) {
        VisitorBatchImportRow row = new VisitorBatchImportRow();
        row.setName(name);
        row.setPhone("13900000001");
        row.setExpectedTime(time);
        row.setLeaveTime(LocalDateTime.parse(time.replace(' ', 'T')).plusHours(2).toString().replace('T', ' '));
        row.setVisitPurpose("业务洽谈");
        return row;
    }

    private MockMultipartFile file(VisitorBatchImportRow row) {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        EasyExcel.write(out, VisitorBatchImportRow.class).sheet().doWrite(List.of(row));
        return new MockMultipartFile("file", "batch.xlsx",
                "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet", out.toByteArray());
    }
}
