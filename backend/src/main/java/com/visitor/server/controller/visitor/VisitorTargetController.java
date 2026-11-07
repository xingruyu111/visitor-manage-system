package com.visitor.server.controller.visitor;

import com.visitor.common.core.Result;
import com.visitor.domain.entity.Staff;
import com.visitor.server.service.VisitorService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@Tag(name = "访客-被访人")
@RestController
@RequestMapping("/web/visitor/targets")
public class VisitorTargetController {

    private final VisitorService visitorService;

    public VisitorTargetController(VisitorService visitorService) {
        this.visitorService = visitorService;
    }

    @Operation(summary = "可预约的被访人")
    @GetMapping
    public Result<List<Staff>> targets(@RequestParam(required = false) String keyword) {
        return Result.success(visitorService.queryTargets(keyword));
    }
}
