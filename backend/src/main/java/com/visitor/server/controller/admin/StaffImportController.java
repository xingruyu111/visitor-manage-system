package com.visitor.server.controller.admin;

import com.alibaba.excel.EasyExcel;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.visitor.common.core.Result;
import com.visitor.common.enums.PersonTypeEnum;
import com.visitor.common.exception.BizException;
import com.visitor.domain.dto.StaffImportRow;
import com.visitor.domain.dto.StaffSaveDTO;
import com.visitor.domain.entity.Department;
import com.visitor.domain.entity.SchoolClass;
import com.visitor.security.SchoolScopes;
import com.visitor.server.mapper.DepartmentMapper;
import com.visitor.server.mapper.SchoolClassMapper;
import com.visitor.server.service.StaffService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Tag(name = "人员导入")
@RestController
@RequestMapping("/web/admin/staff")
public class StaffImportController {

    private final StaffService staffService;
    private final DepartmentMapper departmentMapper;
    private final SchoolClassMapper classMapper;

    public StaffImportController(StaffService staffService, DepartmentMapper departmentMapper,
                                 SchoolClassMapper classMapper) {
        this.staffService = staffService;
        this.departmentMapper = departmentMapper;
        this.classMapper = classMapper;
    }

    @Operation(summary = "Excel 批量导入人员（自动开通账号）")
    @PostMapping("/import")
    public Result<Map<String, Object>> importStaff(@RequestParam("file") MultipartFile file,
                                                   @RequestParam(required = false) Long schoolId) {
        if (file == null || file.isEmpty()) {
            throw new BizException(400, "请选择 Excel 文件");
        }
        Long scope = SchoolScopes.resolve(schoolId);
        List<StaffImportRow> rows;
        try {
            rows = EasyExcel.read(file.getInputStream()).head(StaffImportRow.class).sheet().doReadSync();
        } catch (Exception e) {
            throw new BizException(400, "Excel 解析失败，请使用模板格式");
        }
        int success = 0;
        List<Map<String, Object>> errors = new ArrayList<>();
        for (int i = 0; i < rows.size(); i++) {
            StaffImportRow row = rows.get(i);
            int excelRow = i + 2;
            try {
                staffService.create(scope, toDto(scope, row));
                success++;
            } catch (Exception e) {
                Map<String, Object> error = new LinkedHashMap<>();
                error.put("row", excelRow);
                error.put("personNo", row.getPersonNo());
                error.put("message", e instanceof BizException ? e.getMessage() : "导入失败");
                errors.add(error);
            }
        }
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("total", rows.size());
        result.put("success", success);
        result.put("failed", errors.size());
        result.put("errors", errors);
        return Result.success(result);
    }

    private StaffSaveDTO toDto(Long schoolId, StaffImportRow row) {
        StaffSaveDTO dto = new StaffSaveDTO();
        dto.setPersonNo(row.getPersonNo());
        dto.setName(row.getName());
        dto.setPhone(row.getPhone());
        dto.setPersonType(parseType(row.getPersonType()));
        dto.setDeptId(findDept(schoolId, row.getDeptName()));
        dto.setClassId(findClass(schoolId, row.getClassName()));
        dto.setPosition(row.getPosition());
        return dto;
    }

    private Integer parseType(String value) {
        if (value == null || value.isBlank()) {
            throw new BizException(400, "人员类型不能为空");
        }
        String text = value.trim();
        for (PersonTypeEnum type : PersonTypeEnum.values()) {
            if (String.valueOf(type.getCode()).equals(text) || type.getDescription().equals(text)) {
                return type.getCode();
            }
        }
        throw new BizException(400, "人员类型不合法：" + text);
    }

    private Long findDept(Long schoolId, String name) {
        if (name == null || name.isBlank()) {
            return null;
        }
        Department department = departmentMapper.selectOne(new LambdaQueryWrapper<Department>()
                .eq(Department::getSchoolId, schoolId)
                .eq(Department::getName, name.trim())
                .last("LIMIT 1"));
        if (department == null) {
            throw new BizException(400, "部门不存在：" + name);
        }
        return department.getId();
    }

    private Long findClass(Long schoolId, String name) {
        if (name == null || name.isBlank()) {
            return null;
        }
        SchoolClass schoolClass = classMapper.selectOne(new LambdaQueryWrapper<SchoolClass>()
                .eq(SchoolClass::getSchoolId, schoolId)
                .eq(SchoolClass::getName, name.trim())
                .last("LIMIT 1"));
        if (schoolClass == null) {
            throw new BizException(400, "班级不存在：" + name);
        }
        return schoolClass.getId();
    }
}
