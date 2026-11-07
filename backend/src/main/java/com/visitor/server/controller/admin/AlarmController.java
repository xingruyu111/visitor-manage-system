package com.visitor.server.controller.admin;

import com.visitor.common.core.PageResult;
import com.visitor.common.core.Result;
import com.visitor.domain.entity.AlarmRecord;
import com.visitor.security.UserContext;
import com.visitor.server.service.AlarmService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "通行预警")
@RestController
@RequestMapping("/web/admin/alarms")
public class AlarmController {

    private final AlarmService alarmService;

    public AlarmController(AlarmService alarmService) {
        this.alarmService = alarmService;
    }

    @Operation(summary = "告警分页")
    @GetMapping
    public Result<PageResult<AlarmRecord>> page(@RequestParam(required = false) Integer handled,
                                                @RequestParam(defaultValue = "1") int page,
                                                @RequestParam(defaultValue = "20") int size) {
        return Result.success(alarmService.page(handled, page, size));
    }

    @Operation(summary = "处理告警")
    @PostMapping("/{id}/handle")
    public Result<Void> handle(@PathVariable Long id) {
        alarmService.handle(id, UserContext.getUserId());
        return Result.success();
    }
}
