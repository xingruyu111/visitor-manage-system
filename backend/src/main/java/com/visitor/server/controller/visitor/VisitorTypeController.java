package com.visitor.server.controller.visitor;

import com.visitor.common.core.Result;
import com.visitor.common.exception.BizException;
import com.visitor.domain.entity.Staff;
import com.visitor.domain.entity.VisitorType;
import com.visitor.security.UserContext;
import com.visitor.server.mapper.StaffMapper;
import com.visitor.server.service.AlumniMonthlyPassService;
import com.visitor.server.service.SystemSettingService;
import com.visitor.server.service.VisitorTypeService;
import com.visitor.workflow.ApprovalEngine;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Tag(name = "访客类型")
@RestController
@RequestMapping("/web/visitor")
public class VisitorTypeController {

    private final VisitorTypeService visitorTypeService;
    private final AlumniMonthlyPassService alumniMonthlyPassService;
    private final StaffMapper staffMapper;
    private final ApprovalEngine approvalEngine;
    private final SystemSettingService settingService;

    public VisitorTypeController(VisitorTypeService visitorTypeService,
                                 AlumniMonthlyPassService alumniMonthlyPassService,
                                 StaffMapper staffMapper, ApprovalEngine approvalEngine,
                                 SystemSettingService settingService) {
        this.visitorTypeService = visitorTypeService;
        this.alumniMonthlyPassService = alumniMonthlyPassService;
        this.staffMapper = staffMapper;
        this.approvalEngine = approvalEngine;
        this.settingService = settingService;
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

    @Operation(summary = "预约补充信息要求")
    @GetMapping("/apply-requirements")
    public Result<Map<String, Object>> applyRequirements(
            @RequestParam Long targetId,
            @RequestParam(required = false) String visitorType) {
        Staff target = staffMapper.selectById(targetId);
        if (target == null) {
            throw new BizException(404, "被访人不存在");
        }
        String targetType = target.getPersonType() == null ? null : target.getPersonType().name();
        boolean headTeacherRequired = approvalEngine.requiresHeadTeacher(visitorType, targetType,
                target.getId(), target.getSchoolId(), UserContext.getUserId());
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("targetType", targetType);
        result.put("headTeacherRequired", headTeacherRequired);
        result.put("maxStayHours", settingService.getInt("visit.max_stay_hours", 12));
        return Result.success(result);
    }
}
