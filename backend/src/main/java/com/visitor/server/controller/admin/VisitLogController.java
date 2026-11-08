package com.visitor.server.controller.admin;

import com.visitor.common.core.PageResult;
import com.visitor.common.core.Result;
import com.visitor.domain.vo.VisitLogVO;
import com.visitor.server.service.VisitLogService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDateTime;

@Tag(name = "通行记录")
@RestController
@RequestMapping("/web/admin/visit-logs")
public class VisitLogController {

    private final VisitLogService visitLogService;

    public VisitLogController(VisitLogService visitLogService) {
        this.visitLogService = visitLogService;
    }

    @Operation(summary = "通行记录分页")
    @GetMapping
    public Result<PageResult<VisitLogVO>> page(
            @RequestParam(required = false) String direction,
            @RequestParam(required = false) String source,
            @RequestParam(required = false) @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss") LocalDateTime from,
            @RequestParam(required = false) @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss") LocalDateTime to,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int size) {
        return Result.success(visitLogService.page(direction, source, from, to, page, size));
    }

    @Operation(summary = "导出通行记录（CSV）")
    @GetMapping("/export")
    public void export(
            @RequestParam(required = false) String direction,
            @RequestParam(required = false) String source,
            @RequestParam(required = false) @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss") LocalDateTime from,
            @RequestParam(required = false) @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss") LocalDateTime to,
            HttpServletResponse response) throws java.io.IOException {
        response.setContentType("text/csv;charset=UTF-8");
        response.setHeader("Content-Disposition", "attachment;filename=visit-logs.csv");
        response.getWriter().write('\ufeff');
        response.getWriter().write("时间,方向,来源,访客,车牌,预约ID,外部记录ID\n");
        for (VisitLogVO row : visitLogService.exportRows(direction, source, from, to)) {
            response.getWriter().write(String.join(",",
                    csv(row.getEventTime() == null ? "" : row.getEventTime().toString()),
                    "IN".equals(row.getDirection()) ? "入校" : "离校",
                    csv(row.getSource()),
                    csv(row.getVisitorName()),
                    csv(row.getPlateNumber()),
                    row.getRecordId() == null ? "" : row.getRecordId().toString(),
                    csv(row.getExternalId())) + "\n");
        }
    }

    private String csv(String value) {
        if (value == null) {
            return "";
        }
        return "\"" + value.replace("\"", "\"\"") + "\"";
    }
}
