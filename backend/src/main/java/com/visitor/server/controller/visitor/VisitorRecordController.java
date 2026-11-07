package com.visitor.server.controller.visitor;

import com.visitor.common.core.PageResult;
import com.visitor.common.core.Result;
import com.visitor.domain.dto.VisitorApplyDTO;
import com.visitor.domain.entity.VisitorPass;
import com.visitor.domain.entity.VisitorRecord;
import com.visitor.security.UserContext;
import com.visitor.server.service.VisitorService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@Tag(name = "访客预约")
@RestController
@RequestMapping("/web/visitor/records")
public class VisitorRecordController {

    private final VisitorService visitorService;

    public VisitorRecordController(VisitorService visitorService) {
        this.visitorService = visitorService;
    }

    @Operation(summary = "提交访客申请（自动进入审批）")
    @PostMapping
    public Result<Map<String, Object>> apply(@Valid @RequestBody VisitorApplyDTO dto) {
        return Result.success(visitorService.apply(UserContext.getUserId(), dto));
    }

    @Operation(summary = "我的预约记录")
    @GetMapping
    public Result<PageResult<VisitorRecord>> myRecords(@RequestParam(defaultValue = "1") int page,
                                                       @RequestParam(defaultValue = "20") int size) {
        return Result.success(visitorService.myRecords(UserContext.getUserId(), page, size));
    }

    @Operation(summary = "预约详情")
    @GetMapping("/{id}")
    public Result<VisitorRecord> detail(@PathVariable Long id) {
        return Result.success(visitorService.detail(UserContext.getUserId(), id));
    }

    @Operation(summary = "我的通行凭证")
    @GetMapping("/{id}/pass")
    public Result<VisitorPass> pass(@PathVariable Long id) {
        return Result.success(visitorService.pass(UserContext.getUserId(), id));
    }

    @Operation(summary = "取消预约")
    @PostMapping("/{id}/cancel")
    public Result<Void> cancel(@PathVariable Long id) {
        visitorService.cancel(UserContext.getUserId(), id);
        return Result.success();
    }
}
