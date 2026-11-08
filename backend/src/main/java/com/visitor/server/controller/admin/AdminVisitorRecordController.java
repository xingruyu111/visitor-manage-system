package com.visitor.server.controller.admin;

import com.alibaba.excel.EasyExcel;
import com.visitor.common.core.PageResult;
import com.visitor.common.core.Result;
import com.visitor.domain.dto.VisitorBatchDTO;
import com.visitor.domain.entity.VisitorRecord;
import com.visitor.domain.vo.VisitorRecordExportRow;
import com.visitor.security.SchoolScopes;
import com.visitor.security.UserContext;
import com.visitor.server.service.VisitorService;
import com.visitor.server.mapper.ApprovalInstanceMapper;
import com.visitor.workflow.ApprovalEngine;
import com.visitor.domain.entity.ApprovalInstance;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletResponse;

import java.io.IOException;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@Tag(name = "访客记录（管理端）")
@RestController
@RequestMapping("/web/admin/visitor-records")
public class AdminVisitorRecordController {

    private final VisitorService visitorService;
    private final ApprovalInstanceMapper instanceMapper;
    private final ApprovalEngine approvalEngine;

    public AdminVisitorRecordController(VisitorService visitorService,
                                        ApprovalInstanceMapper instanceMapper, ApprovalEngine approvalEngine) {
        this.visitorService = visitorService;
        this.instanceMapper = instanceMapper;
        this.approvalEngine = approvalEngine;
    }

    @Operation(summary = "全校访客记录分页")
    @GetMapping
    public Result<PageResult<VisitorRecord>> page(@RequestParam(required = false) Long schoolId,
                                                  @RequestParam(required = false) Integer status,
                                                  @RequestParam(required = false) String keyword,
                                                  @RequestParam(defaultValue = "1") int page,
                                                  @RequestParam(defaultValue = "20") int size) {
        return Result.success(visitorService.adminPage(SchoolScopes.resolve(schoolId),
                status, keyword, page, size));
    }

    @Operation(summary = "导出访客记录")
    @GetMapping("/export")
    public void export(@RequestParam(required = false) Integer status,
                       @RequestParam(required = false) String keyword,
                       HttpServletResponse response) throws IOException {
        response.setContentType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet");
        response.setCharacterEncoding("utf-8");
        response.setHeader("Content-Disposition", "attachment;filename=visitor-records.xlsx");
        EasyExcel.write(response.getOutputStream(), VisitorRecordExportRow.class)
                .sheet("访客记录")
                .doWrite(visitorService.exportRows(status, keyword));
    }

    @Operation(summary = "紧急审批（按预约记录）")
    @PostMapping("/{id}/emergency")
    public Result<Void> emergency(@PathVariable Long id,
                                  @RequestParam(defaultValue = "true") boolean approve,
                                  @RequestParam(required = false) String comment) {
        ApprovalInstance instance = instanceMapper.selectOne(new LambdaQueryWrapper<ApprovalInstance>()
                .eq(ApprovalInstance::getBizType, "VISITOR_RECORD")
                .eq(ApprovalInstance::getBizId, id)
                .last("LIMIT 1"));
        if (instance == null) {
            return Result.fail(400, "该记录没有审批实例");
        }
        approvalEngine.emergencyHandle(instance.getId(), approve, comment);
        return Result.success();
    }

    @Operation(summary = "批量直录（无需审批，直接发证）")
    @PostMapping("/batch")
    public Result<Map<String, Object>> batch(@RequestParam(required = false) Long schoolId,
                                             @Valid @RequestBody VisitorBatchDTO dto) {
        int created = visitorService.batchCreate(SchoolScopes.resolve(schoolId), dto,
                UserContext.getUserId());
        return Result.success(Map.of("created", created));
    }
}
