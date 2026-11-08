package com.visitor.server.controller.common;

import com.visitor.common.core.Result;
import com.visitor.common.enums.RoleEnum;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Tag(name = "角色字典")
@RestController
@RequestMapping("/web/common")
public class RoleController {

    @Operation(summary = "角色字典（含是否可在后台分配）")
    @GetMapping("/roles")
    public Result<List<Map<String, Object>>> roles() {
        List<Map<String, Object>> result = Arrays.stream(RoleEnum.values()).map(role -> {
            Map<String, Object> item = new LinkedHashMap<>();
            item.put("code", role.getCode());
            item.put("name", role.getDescription());
            item.put("assignable", role != RoleEnum.SYSTEM_ADMIN && role != RoleEnum.VISITOR);
            return item;
        }).toList();
        return Result.success(result);
    }
}
