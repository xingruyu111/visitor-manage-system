package com.visitor.workflow.web;

import com.visitor.common.core.PageResult;
import com.visitor.common.core.Result;
import com.visitor.domain.entity.ApprovalTask;
import com.visitor.workflow.ApprovalEngine;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * 审批任务接口：所有登录角色可用，引擎内部校验审批人身份。
 */
@Tag(name = "审批任务")
@RestController
@RequestMapping("/web/approval/tasks")
public class ApprovalTaskController {

    private final ApprovalEngine approvalEngine;

    public ApprovalTaskController(ApprovalEngine approvalEngine) {
        this.approvalEngine = approvalEngine;
    }

    @Operation(summary = "我的待审批任务")
    @GetMapping
    public Result<PageResult<ApprovalTask>> myTasks(@RequestParam(defaultValue = "1") int page,
                                                    @RequestParam(defaultValue = "20") int size) {
        return Result.success(approvalEngine.myTasks(page, size));
    }

    @Operation(summary = "通过")
    @PostMapping("/{id}/approve")
    public Result<ApprovalTask> approve(@PathVariable Long id,
                                        @RequestParam(required = false) String comment,
                                        @RequestParam(defaultValue = "false") boolean emergency) {
        return Result.success(approvalEngine.approve(id, comment, emergency));
    }

    @Operation(summary = "驳回")
    @PostMapping("/{id}/reject")
    public Result<ApprovalTask> reject(@PathVariable Long id,
                                       @RequestParam(required = false) String comment,
                                       @RequestParam(defaultValue = "false") boolean emergency) {
        return Result.success(approvalEngine.reject(id, comment, emergency));
    }

    @Operation(summary = "批量通过")
    @PostMapping("/batch-approve")
    public Result<List<ApprovalTask>> batchApprove(@RequestBody BatchApproveRequest request) {
        return Result.success(approvalEngine.batchApprove(request.taskIds(), request.comment()));
    }

    public record BatchApproveRequest(List<Long> taskIds, String comment) {
    }
}
