package com.visitor.server.controller.admin;

import com.alibaba.excel.EasyExcel;
import com.visitor.common.core.PageResult;
import com.visitor.common.core.Result;
import com.visitor.domain.entity.AccessList;
import com.visitor.domain.dto.AccessListImportRow;
import com.visitor.security.SchoolScopes;
import com.visitor.server.service.AccessCheckService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
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

import java.io.IOException;
import java.util.List;

@Tag(name = "黑白名单")
@RestController
@RequestMapping("/web/admin/access-list")
public class AccessListController {

    private final AccessCheckService accessCheckService;

    public AccessListController(AccessCheckService accessCheckService) {
        this.accessCheckService = accessCheckService;
    }

    @Operation(summary = "名单分页")
    @GetMapping
    public Result<PageResult<AccessList>> page(@RequestParam(required = false) Long schoolId,
                                               @RequestParam(required = false) Integer listType,
                                               @RequestParam(required = false) String targetType,
                                               @RequestParam(required = false) String keyword,
                                               @RequestParam(defaultValue = "1") int page,
                                               @RequestParam(defaultValue = "20") int size) {
        return Result.success(accessCheckService.page(SchoolScopes.resolve(schoolId),
                listType, targetType, keyword, page, size));
    }

    @Operation(summary = "新增名单")
    @PostMapping
    public Result<AccessList> create(@RequestParam(required = false) Long schoolId,
                                     @RequestBody AccessList body) {
        return Result.success(accessCheckService.create(SchoolScopes.resolve(schoolId), body));
    }

    @Operation(summary = "修改名单")
    @PutMapping("/{id}")
    public Result<AccessList> update(@RequestParam(required = false) Long schoolId,
                                     @PathVariable Long id,
                                     @RequestBody AccessList body) {
        return Result.success(accessCheckService.update(SchoolScopes.resolve(schoolId), id, body));
    }

    @Operation(summary = "人工提前解除")
    @PostMapping("/{id}/release")
    public Result<AccessList> release(@RequestParam(required = false) Long schoolId,
                                      @PathVariable Long id,
                                      @RequestParam(required = false) String reason) {
        return Result.success(accessCheckService.release(SchoolScopes.resolve(schoolId), id, reason));
    }

    @Operation(summary = "Excel 导入名单")
    @PostMapping("/import")
    public Result<java.util.Map<String, Object>> importExcel(@RequestParam("file") MultipartFile file)
            throws IOException {
        List<AccessListImportRow> rows = EasyExcel.read(file.getInputStream())
                .head(AccessListImportRow.class).sheet().doReadSync();
        return Result.success(accessCheckService.importRows(SchoolScopes.resolve(null), rows));
    }

    @Operation(summary = "删除名单")
    @DeleteMapping("/{id}")
    public Result<Void> delete(@RequestParam(required = false) Long schoolId, @PathVariable Long id) {
        accessCheckService.delete(SchoolScopes.resolve(schoolId), id);
        return Result.success();
    }
}
