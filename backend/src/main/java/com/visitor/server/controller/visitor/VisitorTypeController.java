package com.visitor.server.controller.visitor;

import com.visitor.common.core.Result;
import com.visitor.domain.entity.VisitorType;
import com.visitor.server.service.AlumniMonthlyPassService;
import com.visitor.server.service.VisitorTypeService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

@Tag(name = "访客类型")
@RestController
@RequestMapping("/web/visitor")
public class VisitorTypeController {

    private final VisitorTypeService visitorTypeService;
    private final AlumniMonthlyPassService alumniMonthlyPassService;

    public VisitorTypeController(VisitorTypeService visitorTypeService,
                                 AlumniMonthlyPassService alumniMonthlyPassService) {
        this.visitorTypeService = visitorTypeService;
        this.alumniMonthlyPassService = alumniMonthlyPassService;
    }

    @Operation(summary = "启用中的访客类型")
    @GetMapping("/visitor-types")
    public Result<List<VisitorType>> types() {
        return Result.success(visitorTypeService.list(true));
    }

    @Operation(summary = "当前校友自然月通行资格")
    @GetMapping("/alumni-month")
    public Result<Map<String, Object>> alumniMonth() {
        return Result.success(alumniMonthlyPassService.currentMonth());
    }
}
