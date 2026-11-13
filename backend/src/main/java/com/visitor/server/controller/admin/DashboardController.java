package com.visitor.server.controller.admin;

import com.visitor.common.core.Result;
import com.visitor.server.service.DashboardService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@Tag(name = "工作台统计")
@RestController
@RequestMapping("/web/admin/dashboard")
public class DashboardController {

    private final DashboardService dashboardService;

    public DashboardController(DashboardService dashboardService) {
        this.dashboardService = dashboardService;
    }

    @Operation(summary = "趋势统计（日预约量/通过率/黑名单/接口异常）")
    @GetMapping("/trend")
    public Result<Map<String, Object>> trend(@RequestParam(defaultValue = "14") int days) {
        return Result.success(dashboardService.trend(days));
    }

    @Operation(summary = "月度统计")
    @GetMapping("/monthly")
    public Result<java.util.List<java.util.Map<String, Object>>> monthly(
            @RequestParam(defaultValue = "6") int months) {
        return Result.success(dashboardService.monthly(months));
    }

    @Operation(summary = "数据大屏汇总")
    @GetMapping("/screen")
    public Result<Map<String, Object>> screen() {
        return Result.success(dashboardService.screen());
    }

    @Operation(summary = "统计概览")
    @GetMapping("/summary")
    public Result<Map<String, Object>> summary() {
        return Result.success(dashboardService.summary());
    }
}
