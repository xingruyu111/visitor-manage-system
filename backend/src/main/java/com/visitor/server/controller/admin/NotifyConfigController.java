package com.visitor.server.controller.admin;

import com.visitor.common.core.Result;
import com.visitor.domain.entity.NotifyConfig;
import com.visitor.server.service.NotifyConfigService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@Tag(name = "消息通知配置")
@RestController
@RequestMapping("/web/websuper/notify-config")
public class NotifyConfigController {

    private final NotifyConfigService notifyConfigService;

    public NotifyConfigController(NotifyConfigService notifyConfigService) {
        this.notifyConfigService = notifyConfigService;
    }

    @Operation(summary = "通知配置列表")
    @GetMapping
    public Result<List<NotifyConfig>> list() {
        return Result.success(notifyConfigService.list());
    }

    @Operation(summary = "修改通知配置")
    @PutMapping("/{type}")
    public Result<NotifyConfig> update(@PathVariable String type,
                                       @RequestParam(required = false) Integer enabled,
                                       @RequestParam(required = false) String channels) {
        return Result.success(notifyConfigService.update(type, enabled, channels));
    }
}
