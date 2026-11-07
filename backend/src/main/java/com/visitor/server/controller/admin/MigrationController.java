package com.visitor.server.controller.admin;

import com.alibaba.excel.EasyExcel;
import com.visitor.common.core.Result;
import com.visitor.domain.dto.AccessListImportRow;
import com.visitor.domain.dto.VisitorRecordImportRow;
import com.visitor.server.service.MigrationService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.List;
import java.util.Map;

/**
 * 历史数据迁移：旧系统导出的 Excel 批量写入新库。
 */
@Tag(name = "历史数据迁移")
@RestController
@RequestMapping("/web/websuper/migration")
public class MigrationController {

    private final MigrationService migrationService;

    public MigrationController(MigrationService migrationService) {
        this.migrationService = migrationService;
    }

    @Operation(summary = "导入历史访客记录")
    @PostMapping("/visitor-records")
    public Result<Map<String, Object>> importVisitorRecords(@RequestParam("file") MultipartFile file)
            throws IOException {
        List<VisitorRecordImportRow> rows = EasyExcel.read(file.getInputStream())
                .head(VisitorRecordImportRow.class).sheet().doReadSync();
        return Result.success(migrationService.importVisitorRecords(rows));
    }

    @Operation(summary = "导入历史名单")
    @PostMapping("/access-list")
    public Result<Map<String, Object>> importAccessList(@RequestParam("file") MultipartFile file)
            throws IOException {
        List<AccessListImportRow> rows = EasyExcel.read(file.getInputStream())
                .head(AccessListImportRow.class).sheet().doReadSync();
        return Result.success(migrationService.importAccessList(rows));
    }
}
