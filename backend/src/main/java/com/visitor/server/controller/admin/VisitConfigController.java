package com.visitor.server.controller.admin;

import com.visitor.common.core.Result;
import com.visitor.domain.dto.VisitorTypeSaveDTO;
import com.visitor.domain.entity.VisitorType;
import com.visitor.server.service.VisitConfigService;
import com.visitor.server.service.VisitorTypeService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

@Tag(name = "预约配置")
@RestController
@RequestMapping("/web/admin")
public class VisitConfigController {

    private final VisitorTypeService visitorTypeService;
    private final VisitConfigService visitConfigService;

    public VisitConfigController(VisitorTypeService visitorTypeService, VisitConfigService visitConfigService) {
        this.visitorTypeService = visitorTypeService;
        this.visitConfigService = visitConfigService;
    }

    @Operation(summary = "访客类型列表")
    @GetMapping("/visitor-types")
    public Result<List<VisitorType>> types() {
        return Result.success(visitorTypeService.list(false));
    }

    @Operation(summary = "新增访客类型")
    @PostMapping("/visitor-types")
    public Result<VisitorType> create(@Valid @RequestBody VisitorTypeSaveDTO dto) {
        return Result.success(visitorTypeService.create(dto));
    }

    @Operation(summary = "修改访客类型")
    @PutMapping("/visitor-types/{id}")
    public Result<VisitorType> update(@PathVariable Long id, @Valid @RequestBody VisitorTypeSaveDTO dto) {
        return Result.success(visitorTypeService.update(id, dto));
    }

    @Operation(summary = "启用/停用访客类型")
    @PutMapping("/visitor-types/{id}/status")
    public Result<VisitorType> status(@PathVariable Long id, @RequestParam Integer status) {
        return Result.success(visitorTypeService.updateStatus(id, status));
    }

    @Operation(summary = "校友通行月份")
    @GetMapping("/visit-config/alumni-months")
    public Result<Map<String, Object>> alumniMonths() {
        return Result.success(Map.of("months", visitConfigService.alumniMonths()));
    }

    @Operation(summary = "保存校友通行月份")
    @PutMapping("/visit-config/alumni-months")
    public Result<Map<String, Object>> updateAlumniMonths(@RequestBody Map<String, List<Integer>> body) {
        visitConfigService.updateAlumniMonths(body.get("months"));
        return Result.success(Map.of("months", visitConfigService.alumniMonths()));
    }
}
