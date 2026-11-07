package com.visitor.server.controller.admin;

import com.visitor.common.core.Result;
import com.visitor.domain.dto.SchoolClassSaveDTO;
import com.visitor.domain.vo.SchoolClassVO;
import com.visitor.security.SchoolScopes;
import com.visitor.server.service.SchoolClassService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@Tag(name = "班级管理")
@RestController
@RequestMapping("/web/admin/classes")
public class SchoolClassController {

    private final SchoolClassService classService;

    public SchoolClassController(SchoolClassService classService) {
        this.classService = classService;
    }

    @Operation(summary = "班级列表")
    @GetMapping
    public Result<List<SchoolClassVO>> list(@RequestParam(required = false) Long schoolId) {
        return Result.success(classService.list(SchoolScopes.resolve(schoolId)));
    }

    @Operation(summary = "新增班级")
    @PostMapping
    public Result<SchoolClassVO> create(@RequestParam(required = false) Long schoolId,
                                        @Valid @RequestBody SchoolClassSaveDTO dto) {
        return Result.success(classService.create(SchoolScopes.resolve(schoolId), dto));
    }

    @Operation(summary = "修改班级")
    @PutMapping("/{id}")
    public Result<SchoolClassVO> update(@RequestParam(required = false) Long schoolId,
                                        @PathVariable Long id,
                                        @Valid @RequestBody SchoolClassSaveDTO dto) {
        return Result.success(classService.update(SchoolScopes.resolve(schoolId), id, dto));
    }
}
