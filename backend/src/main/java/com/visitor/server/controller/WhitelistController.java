package com.visitor.server.controller;

import com.visitor.common.core.PageResult;
import com.visitor.common.core.Result;
import com.visitor.domain.dto.WhitelistSaveDTO;
import com.visitor.domain.entity.AccessList;
import com.visitor.server.service.WhitelistService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.util.Map;

@Tag(name = "人员白名单")
@RestController
@RequestMapping("/web/whitelist")
public class WhitelistController {

    private final WhitelistService whitelistService;

    public WhitelistController(WhitelistService whitelistService) {
        this.whitelistService = whitelistService;
    }

    @Operation(summary = "白名单分页")
    @GetMapping
    public Result<PageResult<AccessList>> page(@RequestParam(required = false) String keyword,
                                               @RequestParam(defaultValue = "1") int page,
                                               @RequestParam(defaultValue = "20") int size) {
        return Result.success(whitelistService.page(keyword, page, size));
    }

    @Operation(summary = "我导入的白名单")
    @GetMapping("/imported")
    public Result<PageResult<AccessList>> imported(@RequestParam(defaultValue = "1") int page,
                                                   @RequestParam(defaultValue = "50") int size) {
        return Result.success(whitelistService.importedPage(page, size));
    }

    @Operation(summary = "新增白名单")
    @PostMapping
    public Result<AccessList> create(@Valid @RequestBody WhitelistSaveDTO dto) {
        return Result.success(whitelistService.create(dto));
    }

    @Operation(summary = "修改白名单")
    @PutMapping("/{id}")
    public Result<AccessList> update(@PathVariable Long id, @Valid @RequestBody WhitelistSaveDTO dto) {
        return Result.success(whitelistService.update(id, dto));
    }

    @Operation(summary = "审批通过")
    @PostMapping("/{id}/approve")
    public Result<AccessList> approve(@PathVariable Long id) {
        return Result.success(whitelistService.approve(id));
    }

    @Operation(summary = "审批驳回")
    @PostMapping("/{id}/reject")
    public Result<AccessList> reject(@PathVariable Long id,
                                     @RequestParam(required = false) String reason) {
        return Result.success(whitelistService.reject(id, reason));
    }

    @Operation(summary = "启用/停用白名单")
    @PutMapping("/{id}/status")
    public Result<AccessList> status(@PathVariable Long id, @RequestParam Integer status) {
        return Result.success(whitelistService.updateStatus(id, status));
    }

    @Operation(summary = "删除白名单")
    @DeleteMapping("/{id}")
    public Result<Void> delete(@PathVariable Long id) {
        whitelistService.delete(id);
        return Result.success();
    }

    @Operation(summary = "Excel 导入白名单")
    @PostMapping("/import")
    public Result<Map<String, Object>> importExcel(@RequestParam("file") MultipartFile file) {
        return Result.success(whitelistService.importExcel(file));
    }
}
