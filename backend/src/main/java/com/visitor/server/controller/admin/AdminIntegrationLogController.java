package com.visitor.server.controller.admin;

import com.visitor.common.core.PageResult;
import com.visitor.common.core.Result;
import com.visitor.domain.entity.IntegrationLog;
import com.visitor.server.service.InterfaceLogService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "接口调用日志")
@RestController
@RequestMapping("/web/admin/integration-logs")
public class AdminIntegrationLogController {

    private final InterfaceLogService integrationLogService;

    public AdminIntegrationLogController(InterfaceLogService integrationLogService) {
        this.integrationLogService = integrationLogService;
    }

    @Operation(summary = "接口日志分页")
    @GetMapping
    public Result<PageResult<IntegrationLog>> page(@RequestParam(required = false) String integrationKey,
                                                   @RequestParam(required = false) Integer success,
                                                   @RequestParam(defaultValue = "1") int page,
                                                   @RequestParam(defaultValue = "20") int size) {
        return Result.success(integrationLogService.page(integrationKey, success, page, size));
    }

    @Operation(summary = "导出接口异常记录（CSV）")
    @GetMapping("/export")
    public void export(@RequestParam(required = false) String integrationKey,
                       @RequestParam(required = false) Integer success,
                       HttpServletResponse response) throws java.io.IOException {
        response.setContentType("text/csv;charset=UTF-8");
        response.setHeader("Content-Disposition", "attachment;filename=integration-logs.csv");
        response.getWriter().write('\ufeff');
        response.getWriter().write("时间,接口,动作,方向,成功,HTTP状态,错误码,错误信息,耗时ms\n");
        for (IntegrationLog log : integrationLogService.exportRows(integrationKey, success)) {
            response.getWriter().write(String.join(",",
                    csv(log.getCreatedAt() == null ? "" : log.getCreatedAt().toString()),
                    csv(log.getIntegrationKey()),
                    csv(log.getAction()),
                    csv(log.getDirection()),
                    String.valueOf(log.getSuccess()),
                    log.getHttpStatus() == null ? "" : log.getHttpStatus().toString(),
                    csv(log.getErrorCode()),
                    csv(log.getErrorMsg()),
                    log.getCostMs() == null ? "" : log.getCostMs().toString()) + "\n");
        }
    }

    private String csv(String value) {
        if (value == null) {
            return "";
        }
        return "\"" + value.replace("\"", "\"\"") + "\"";
    }
}
