package com.visitor.workflow.web;

import com.visitor.common.core.PageResult;
import com.visitor.common.core.Result;
import com.visitor.domain.entity.ApprovalInstance;
import com.visitor.security.SchoolScopes;
import com.visitor.workflow.ApprovalEngine;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "审批实例")
@RestController
@RequestMapping("/web/approval/instances")
public class ApprovalInstanceController {

    private final ApprovalEngine approvalEngine;

    public ApprovalInstanceController(ApprovalEngine approvalEngine) {
        this.approvalEngine = approvalEngine;
    }

    @Operation(summary = "审批实例分页")
    @GetMapping
    public Result<PageResult<ApprovalInstance>> page(@RequestParam(required = false) Long schoolId,
                                                     @RequestParam(required = false) String status,
                                                     @RequestParam(defaultValue = "1") int page,
                                                     @RequestParam(defaultValue = "20") int size) {
        return Result.success(approvalEngine.instances(SchoolScopes.resolve(schoolId), status, page, size));
    }

    @Operation(summary = "取消审批")
    @PostMapping("/{id}/cancel")
    public Result<Void> cancel(@PathVariable Long id, @RequestParam(required = false) String reason) {
        approvalEngine.cancel(id, reason);
        return Result.success();
    }
}
