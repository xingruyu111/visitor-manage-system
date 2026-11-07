package com.visitor.server.controller.guard;

import com.visitor.common.core.Result;
import com.visitor.domain.entity.VisitorRecord;
import com.visitor.server.service.VisitorService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * 车主查询：保安输入车牌查询当天或最新有效预约。
 */
@Tag(name = "车主查询")
@RestController
@RequestMapping("/web/guard/query")
public class GuardQueryController {

    private final VisitorService visitorService;

    public GuardQueryController(VisitorService visitorService) {
        this.visitorService = visitorService;
    }

    @Operation(summary = "按车牌查询预约")
    @GetMapping
    public Result<List<VisitorRecord>> byPlate(@RequestParam String plate) {
        return Result.success(visitorService.queryByPlate(plate));
    }
}
