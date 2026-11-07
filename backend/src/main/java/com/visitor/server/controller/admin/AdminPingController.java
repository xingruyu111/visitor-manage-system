package com.visitor.server.controller.admin;

import com.visitor.common.core.Result;
import com.visitor.security.UserContext;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * 学校管理员接口占位，用于验证 `/web/admin/**` 的角色与学校上下文约束。
 */
@Tag(name = "学校管理员")
@RestController
@RequestMapping("/web/admin")
public class AdminPingController {

    @Operation(summary = "当前管理员上下文")
    @GetMapping("/ping")
    public Result<Map<String, Object>> ping() {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("userId", UserContext.getUserId());
        body.put("role", UserContext.getRole() == null ? null : UserContext.getRole().getCode());
        body.put("schoolId", UserContext.getSchoolId());
        return Result.success(body);
    }
}
