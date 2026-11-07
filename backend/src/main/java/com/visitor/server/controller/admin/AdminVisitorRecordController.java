package com.visitor.server.controller.admin;

import com.visitor.common.core.PageResult;
import com.visitor.common.core.Result;
import com.visitor.domain.dto.VisitorBatchDTO;
import com.visitor.domain.entity.VisitorRecord;
import com.visitor.security.SchoolScopes;
import com.visitor.security.UserContext;
import com.visitor.server.service.VisitorService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
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

    public AdminVisitorRecordController(VisitorService visitorService) {
        this.visitorService = visitorService;
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

    @Operation(summary = "批量直录（无需审批，直接发证）")
    @PostMapping("/batch")
    public Result<Map<String, Object>> batch(@RequestParam(required = false) Long schoolId,
                                             @Valid @RequestBody VisitorBatchDTO dto) {
        int created = visitorService.batchCreate(SchoolScopes.resolve(schoolId), dto,
                UserContext.getUserId());
        return Result.success(Map.of("created", created));
    }
}
