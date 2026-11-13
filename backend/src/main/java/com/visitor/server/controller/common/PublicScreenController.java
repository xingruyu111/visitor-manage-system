package com.visitor.server.controller.common;

import com.visitor.common.core.Result;
import com.visitor.server.service.DashboardService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

/**
 * 数据大屏公开数据。大屏链接无需登录即可访问，不返回访客手机号等敏感字段。
 */
@Tag(name = "公开数据")
@RestController
@RequestMapping("/web/public")
public class PublicScreenController {

    private final DashboardService dashboardService;

    public PublicScreenController(DashboardService dashboardService) {
        this.dashboardService = dashboardService;
    }

    @Operation(summary = "数据大屏公开汇总")
    @GetMapping("/screen")
    public Result<Map<String, Object>> screen() {
        return Result.success(dashboardService.screen());
    }
}
