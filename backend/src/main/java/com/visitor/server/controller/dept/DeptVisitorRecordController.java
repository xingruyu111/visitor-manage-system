package com.visitor.server.controller.dept;

import com.visitor.common.core.Result;
import com.visitor.domain.dto.VisitorBatchDTO;
import com.visitor.security.SchoolScopes;
import com.visitor.security.UserContext;
import com.visitor.server.service.VisitorService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

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
}
