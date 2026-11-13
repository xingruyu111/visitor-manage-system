package com.visitor.server.controller.dept;

import com.alibaba.excel.EasyExcel;
import com.visitor.common.core.Result;
import com.visitor.common.exception.BizException;
import com.visitor.domain.dto.VisitorBatchDTO;
import com.visitor.domain.dto.VisitorBatchImportRow;
import com.visitor.security.SchoolScopes;
import com.visitor.security.UserContext;
import com.visitor.server.service.VisitorService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 部门发起批量预约：创建待审批记录，流转保卫处审批。
 */
@Tag(name = "部门批量预约")
@RestController
@RequestMapping("/web/dept/visitor-records")
public class DeptVisitorRecordController {

    private final VisitorService visitorService;

    public DeptVisitorRecordController(VisitorService visitorService) {
        this.visitorService = visitorService;
    }

    @Operation(summary = "部门批量发起预约")
    @PostMapping("/batch")
    public Result<Map<String, Object>> batch(@Valid @RequestBody VisitorBatchDTO dto) {
        int created = visitorService.deptBatch(SchoolScopes.resolve(null), dto, UserContext.getUserId());
        return Result.success(Map.of("created", created));
    }

    @Operation(summary = "批量导入记录")
    @GetMapping("/batch/imported")
    public Result<com.visitor.common.core.PageResult<Map<String, Object>>> imported(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "50") int size) {
        return Result.success(visitorService.batchImported(page, size));
    }

    @Operation(summary = "Excel 批量发起预约")
    @PostMapping("/batch/import")
    public Result<Map<String, Object>> importBatch(@RequestParam("file") MultipartFile file) {
        List<VisitorBatchImportRow> rows;
        try {
            rows = EasyExcel.read(file.getInputStream())
                    .head(VisitorBatchImportRow.class).sheet().doReadSync();
        } catch (IOException e) {
            throw new BizException(400, "Excel 读取失败");
        }
        int success = 0;
        List<Map<String, Object>> errors = new ArrayList<>();
        for (int i = 0; i < rows.size(); i++) {
            VisitorBatchImportRow row = rows.get(i);
            try {
                VisitorBatchDTO.Item item = new VisitorBatchDTO.Item();
                item.setName(row.getName());
                item.setPhone(row.getPhone());
                item.setVisitPurpose(row.getVisitPurpose());
                item.setExpectedTime(parseTime(row.getExpectedTime()));
                item.setLeaveTime(parseTime(row.getLeaveTime()));
                VisitorBatchDTO dto = new VisitorBatchDTO();
                dto.setItems(List.of(item));
                visitorService.deptBatch(SchoolScopes.resolve(null), dto, UserContext.getUserId());
                success++;
            } catch (Exception e) {
                Map<String, Object> error = new LinkedHashMap<>();
                error.put("row", i + 2);
                error.put("reason", e.getMessage() == null ? "导入失败" : e.getMessage());
                errors.add(error);
            }
        }
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("success", success);
        result.put("failed", errors.size());
        result.put("errors", errors);
        return Result.success(result);
    }

    private LocalDateTime parseTime(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        String text = value.trim().replace("T", " ");
        for (String pattern : List.of("yyyy-MM-dd HH:mm:ss", "yyyy-MM-dd HH:mm")) {
            try {
                return LocalDateTime.parse(text, DateTimeFormatter.ofPattern(pattern));
            } catch (Exception ignored) {
                // 继续尝试下一种格式
            }
        }
        return LocalDate.parse(text).atStartOfDay();
    }
}
