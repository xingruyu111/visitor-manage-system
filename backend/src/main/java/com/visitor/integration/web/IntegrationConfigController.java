package com.visitor.integration.web;

import com.visitor.common.core.Result;
import com.visitor.domain.dto.IntegrationConfigSaveDTO;
import com.visitor.domain.vo.IntegrationConfigVO;
import com.visitor.integration.config.IntegrationConfigService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

/**
 * 系统集成配置管理（M2 接入真实鉴权后仅 SUPER 可访问）。
 */
@Tag(name = "系统集成配置")
@RestController
@RequestMapping("/web/websuper/integration-configs")
public class IntegrationConfigController {

    private final IntegrationConfigService configService;

    public IntegrationConfigController(IntegrationConfigService configService) {
        this.configService = configService;
    }

    @Operation(summary = "集成配置列表（密钥脱敏）")
    @GetMapping
    public Result<List<IntegrationConfigVO>> list() {
        return Result.success(configService.list());
    }

    @Operation(summary = "集成配置详情")
    @GetMapping("/{key}")
    public Result<IntegrationConfigVO> detail(@PathVariable String key) {
        return Result.success(configService.get(key));
    }

    @Operation(summary = "保存集成配置（密钥留空表示不修改）")
    @PutMapping("/{key}")
    public Result<IntegrationConfigVO> save(@PathVariable String key,
                                            @Valid @RequestBody IntegrationConfigSaveDTO dto) {
        return Result.success(configService.save(key, dto));
    }

    @Operation(summary = "连通性测试")
    @PostMapping("/{key}/test")
    public Result<Map<String, Object>> test(@PathVariable String key) {
        return Result.success(configService.test(key));
    }
}
