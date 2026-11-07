package com.visitor.integration.web;

import com.visitor.common.core.PageResult;
import com.visitor.common.core.Result;
import com.visitor.domain.entity.IntegrationLog;
import com.visitor.integration.core.IntegrationLogService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "系统集成日志")
@RestController
@RequestMapping("/web/websuper/integration-logs")
public class IntegrationLogController {

    private final IntegrationLogService logService;

    public IntegrationLogController(IntegrationLogService logService) {
        this.logService = logService;
    }

    @Operation(summary = "集成调用日志分页")
    @GetMapping
    public Result<PageResult<IntegrationLog>> page(
            @RequestParam(required = false) String integrationKey,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int size) {
        return Result.success(logService.page(integrationKey, page, size));
    }
}
