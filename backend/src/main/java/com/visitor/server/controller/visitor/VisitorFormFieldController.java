package com.visitor.server.controller.visitor;

import com.visitor.common.core.Result;
import com.visitor.domain.entity.FormField;
import com.visitor.server.service.FormFieldService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * 访客端读取差异化表单字段。
 */
@Tag(name = "访客-表单字段")
@RestController
@RequestMapping("/web/visitor/form-fields")
public class VisitorFormFieldController {

    private final FormFieldService formFieldService;

    public VisitorFormFieldController(FormFieldService formFieldService) {
        this.formFieldService = formFieldService;
    }

    @Operation(summary = "按访客类型获取表单字段")
    @GetMapping
    public Result<List<FormField>> list(@RequestParam(required = false) String visitorType) {
        return Result.success(formFieldService.list(visitorType, true));
    }
}
