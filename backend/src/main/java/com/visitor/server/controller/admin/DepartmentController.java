package com.visitor.server.controller.admin;

import com.visitor.common.core.Result;
import com.visitor.domain.dto.DepartmentSaveDTO;
import com.visitor.domain.vo.DepartmentVO;
import com.visitor.security.SchoolScopes;
import com.visitor.server.service.DepartmentService;
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

import java.util.List;

@Tag(name = "部门管理")
@RestController
@RequestMapping("/web/admin/departments")
public class DepartmentController {

    private final DepartmentService departmentService;

    public DepartmentController(DepartmentService departmentService) {
        this.departmentService = departmentService;
    }

    @Operation(summary = "部门树")
    @GetMapping
    public Result<List<DepartmentVO>> tree(@RequestParam(required = false) Long schoolId) {
        return Result.success(departmentService.tree(SchoolScopes.resolve(schoolId)));
    }

    @Operation(summary = "新增部门")
    @PostMapping
    public Result<DepartmentVO> create(@RequestParam(required = false) Long schoolId,
                                       @Valid @RequestBody DepartmentSaveDTO dto) {
        return Result.success(departmentService.create(SchoolScopes.resolve(schoolId), dto));
    }

    @Operation(summary = "修改部门")
    @PutMapping("/{id}")
    public Result<DepartmentVO> update(@RequestParam(required = false) Long schoolId,
                                       @PathVariable Long id,
                                       @Valid @RequestBody DepartmentSaveDTO dto) {
        return Result.success(departmentService.update(SchoolScopes.resolve(schoolId), id, dto));
    }

    @Operation(summary = "删除部门")
    @DeleteMapping("/{id}")
    public Result<Void> delete(@RequestParam(required = false) Long schoolId, @PathVariable Long id) {
        departmentService.delete(SchoolScopes.resolve(schoolId), id);
        return Result.success();
    }
}
