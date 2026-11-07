package com.visitor.server.controller.admin;

import com.visitor.common.core.PageResult;
import com.visitor.common.core.Result;
import com.visitor.domain.dto.StaffSaveDTO;
import com.visitor.domain.vo.StaffVO;
import com.visitor.security.SchoolScopes;
import com.visitor.server.service.StaffService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "人员管理")
@RestController
@RequestMapping("/web/admin/staff")
public class StaffController {

    private final StaffService staffService;

    public StaffController(StaffService staffService) {
        this.staffService = staffService;
    }

    @Operation(summary = "人员分页")
    @GetMapping
    public Result<PageResult<StaffVO>> page(@RequestParam(required = false) Long schoolId,
                                            @RequestParam(required = false) String keyword,
                                            @RequestParam(required = false) Integer personType,
                                            @RequestParam(required = false) Long deptId,
                                            @RequestParam(defaultValue = "1") int page,
                                            @RequestParam(defaultValue = "20") int size) {
        return Result.success(staffService.page(SchoolScopes.resolve(schoolId), keyword,
                personType, deptId, page, size));
    }

    @Operation(summary = "人员详情")
    @GetMapping("/{id}")
    public Result<StaffVO> detail(@RequestParam(required = false) Long schoolId, @PathVariable Long id) {
        return Result.success(staffService.get(SchoolScopes.resolve(schoolId), id));
    }

    @Operation(summary = "新增人员")
    @PostMapping
    public Result<StaffVO> create(@RequestParam(required = false) Long schoolId,
                                  @Valid @RequestBody StaffSaveDTO dto) {
        return Result.success(staffService.create(SchoolScopes.resolve(schoolId), dto));
    }

    @Operation(summary = "修改人员")
    @PutMapping("/{id}")
    public Result<StaffVO> update(@RequestParam(required = false) Long schoolId,
                                  @PathVariable Long id,
                                  @Valid @RequestBody StaffSaveDTO dto) {
        return Result.success(staffService.update(SchoolScopes.resolve(schoolId), id, dto));
    }

    @Operation(summary = "停用人员（离职/毕业，同时回收账号角色）")
    @DeleteMapping("/{id}")
    public Result<Void> deactivate(@RequestParam(required = false) Long schoolId, @PathVariable Long id) {
        staffService.deactivate(SchoolScopes.resolve(schoolId), id);
        return Result.success();
    }
}
