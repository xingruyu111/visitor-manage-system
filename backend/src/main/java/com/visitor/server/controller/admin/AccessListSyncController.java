package com.visitor.server.controller.admin;

import com.visitor.common.core.Result;
import com.visitor.server.service.AccessListSyncService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@Tag(name = "名单双向同步")
@RestController
@RequestMapping("/web/websuper/access-list-sync")
public class AccessListSyncController {

    private final AccessListSyncService accessListSyncService;

    public AccessListSyncController(AccessListSyncService accessListSyncService) {
        this.accessListSyncService = accessListSyncService;
    }

    @Operation(summary = "从外部系统拉取名单")
    @PostMapping("/pull")
    public Result<Map<String, Object>> pull() {
        return Result.success(accessListSyncService.pull());
    }

    @Operation(summary = "推送本地名单到外部系统")
    @PostMapping("/push")
    public Result<Map<String, Object>> push() {
        return Result.success(accessListSyncService.push());
    }
}
