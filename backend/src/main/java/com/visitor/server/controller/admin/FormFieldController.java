package com.visitor.server.controller.admin;

import com.visitor.common.core.Result;
import com.visitor.domain.entity.FormField;
import com.visitor.server.service.FormFieldService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
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

@Tag(name = "差异化表单字段")
@RestController
@RequestMapping("/web/admin/form-fields")
public class FormFieldController {

    private final FormFieldService formFieldService;

    public FormFieldController(FormFieldService formFieldService) {
        this.formFieldService = formFieldService;
    }

    @Operation(summary = "字段列表")
    @GetMapping
    public Result<List<FormField>> list(@RequestParam(required = false) String visitorType,
                                        @RequestParam(defaultValue = "false") boolean onlyActive) {
        return Result.success(formFieldService.list(visitorType, onlyActive));
    }

    @Operation(summary = "新增字段")
    @PostMapping
    public Result<FormField> create(@RequestBody FormField body) {
        return Result.success(formFieldService.create(body));
    }

    @Operation(summary = "修改字段")
    @PutMapping("/{id}")
    public Result<FormField> update(@PathVariable Long id, @RequestBody FormField body) {
        return Result.success(formFieldService.update(id, body));
    }

    @Operation(summary = "删除字段")
    @DeleteMapping("/{id}")
    public Result<Void> delete(@PathVariable Long id) {
        formFieldService.delete(id);
        return Result.success();
    }
}
