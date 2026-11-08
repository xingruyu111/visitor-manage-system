package com.visitor.server.controller.admin;

import com.visitor.common.core.Result;
import com.visitor.domain.entity.SystemSetting;
import com.visitor.server.service.SystemSettingService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@Tag(name = "系统参数")
@RestController
@RequestMapping("/web/websuper/settings")
public class SystemSettingController {

    private final SystemSettingService settingService;

    public SystemSettingController(SystemSettingService settingService) {
        this.settingService = settingService;
    }

    @Operation(summary = "参数列表")
    @GetMapping
    public Result<List<SystemSetting>> list() {
        return Result.success(settingService.list());
    }

    @Operation(summary = "修改参数")
    @PutMapping("/{key}")
    public Result<SystemSetting> update(@PathVariable String key, @RequestParam String value) {
        return Result.success(settingService.update(key, value));
    }
}
