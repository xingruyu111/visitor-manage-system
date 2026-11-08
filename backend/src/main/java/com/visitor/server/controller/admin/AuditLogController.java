package com.visitor.server.controller.admin;

import com.visitor.common.core.PageResult;
import com.visitor.common.core.Result;
import com.visitor.domain.entity.AuditLog;
import com.visitor.server.service.AuditLogService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "操作审计日志")
@RestController
@RequestMapping("/web/admin/audit-logs")
public class AuditLogController {

    private final AuditLogService auditLogService;

    public AuditLogController(AuditLogService auditLogService) {
        this.auditLogService = auditLogService;
    }

    @Operation(summary = "审计日志分页")
    @GetMapping
    public Result<PageResult<AuditLog>> page(@RequestParam(required = false) String action,
                                             @RequestParam(required = false) String bizType,
                                             @RequestParam(required = false) String keyword,
                                             @RequestParam(defaultValue = "1") int page,
                                             @RequestParam(defaultValue = "20") int size) {
        return Result.success(auditLogService.page(action, bizType, keyword, page, size));
    }

    @Operation(summary = "导出审计日志（CSV）")
    @GetMapping("/export")
    public void export(@RequestParam(required = false) String action,
                       @RequestParam(required = false) String bizType,
                       @RequestParam(required = false) String keyword,
                       HttpServletResponse response) throws java.io.IOException {
        response.setContentType("text/csv;charset=UTF-8");
        response.setHeader("Content-Disposition", "attachment;filename=audit-logs.csv");
        response.getWriter().write('\ufeff');
        response.getWriter().write("时间,操作人,动作,业务类型,业务ID,详情,IP\n");
        for (AuditLog log : auditLogService.exportRows(action, bizType, keyword)) {
            response.getWriter().write(String.join(",",
                    csv(log.getCreatedAt() == null ? "" : log.getCreatedAt().toString()),
                    csv(log.getUserName()),
                    csv(log.getAction()),
                    csv(log.getBizType()),
                    log.getBizId() == null ? "" : log.getBizId().toString(),
                    csv(log.getDetail()),
                    csv(log.getIp())) + "\n");
        }
    }

    private String csv(String value) {
        if (value == null) {
            return "";
        }
        return "\"" + value.replace("\"", "\"\"") + "\"";
    }
}
