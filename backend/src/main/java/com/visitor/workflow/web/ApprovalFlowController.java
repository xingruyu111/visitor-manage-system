package com.visitor.workflow.web;

import com.visitor.common.core.Result;
import com.visitor.domain.entity.ApprovalFlow;
import com.visitor.security.UserContext;
import com.visitor.workflow.ApprovalFlowService;
import com.visitor.workflow.FlowDefinition;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * 审批流程设计器接口（仅 SUPER）。
 */
@Tag(name = "审批流程")
@RestController
@RequestMapping("/web/websuper/approval-flows")
public class ApprovalFlowController {

    private final ApprovalFlowService flowService;

    public ApprovalFlowController(ApprovalFlowService flowService) {
        this.flowService = flowService;
    }

    @Operation(summary = "流程列表（含草稿/已发布）")
    @GetMapping
    public Result<List<ApprovalFlow>> list() {
        return Result.success(flowService.list());
    }

    @Operation(summary = "流程详情")
    @GetMapping("/{code}")
    public Result<ApprovalFlow> detail(@PathVariable String code,
                                       @RequestParam(required = false) Integer version) {
        return Result.success(flowService.detail(code, version));
    }

    @Operation(summary = "保存草稿")
    @PutMapping("/{code}/draft")
    public Result<ApprovalFlow> saveDraft(@PathVariable String code,
                                          @RequestBody FlowDefinition definition) {
        return Result.success(flowService.saveDraft(code, definition, UserContext.getUserId()));
    }

    @Operation(summary = "校验流程")
    @PostMapping("/{code}/validate")
    public Result<List<String>> validate(@PathVariable String code) {
        return Result.success(flowService.validate(code));
    }

    @Operation(summary = "发布流程")
    @PostMapping("/{code}/publish")
    public Result<ApprovalFlow> publish(@PathVariable String code) {
        return Result.success(flowService.publish(code, UserContext.getUserId()));
    }

    @Operation(summary = "发布指定历史版本")
    @PostMapping("/{code}/versions/{version}/publish")
    public Result<ApprovalFlow> publishVersion(@PathVariable String code, @PathVariable Integer version) {
        return Result.success(flowService.publishVersion(code, version, UserContext.getUserId()));
    }
    @Operation(summary = "停用流程")
    @PostMapping("/{code}/offline")
    public Result<ApprovalFlow> offline(@PathVariable String code) {
        return Result.success(flowService.offline(code, UserContext.getUserId()));
    }

    @Operation(summary = "版本历史")
    @GetMapping("/{code}/versions")
    public Result<List<ApprovalFlow>> versions(@PathVariable String code) {
        return Result.success(flowService.versions(code));
    }

    @Operation(summary = "预览节点与预计审批人")
    @PostMapping("/{code}/preview")
    public Result<List<ApprovalFlowService.PreviewNode>> preview(
            @PathVariable String code,
            @RequestParam(required = false) Long targetId) {
        return Result.success(flowService.preview(code, targetId));
    }
}
