package com.visitor.server.controller.admin;

import com.visitor.common.core.PageResult;
import com.visitor.common.core.Result;
import com.visitor.domain.dto.UserCreateDTO;
import com.visitor.server.service.UserAdminService;
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

import java.util.Map;

@Tag(name = "账号管理")
@RestController
@RequestMapping("/web/websuper/users")
public class UserAdminController {

    private final UserAdminService userAdminService;

    public UserAdminController(UserAdminService userAdminService) {
        this.userAdminService = userAdminService;
    }

    @Operation(summary = "账号分页")
    @GetMapping
    public Result<PageResult<Map<String, Object>>> page(@RequestParam(required = false) String keyword,
                                                        @RequestParam(required = false) Integer role,
                                                        @RequestParam(defaultValue = "1") int page,
                                                        @RequestParam(defaultValue = "20") int size) {
        return Result.success(userAdminService.page(keyword, role, page, size));
    }

    @Operation(summary = "新增账号")
    @PostMapping
    public Result<Map<String, Object>> create(@Valid @RequestBody UserCreateDTO dto) {
        return Result.success(userAdminService.create(dto));
    }

    @Operation(summary = "启用/停用")
    @PutMapping("/{id}/status")
    public Result<Void> status(@PathVariable Long id, @RequestParam Integer status) {
        userAdminService.updateStatus(id, status);
        return Result.success();
    }

    @Operation(summary = "添加角色")
    @PostMapping("/{id}/roles")
    public Result<Void> addRole(@PathVariable Long id, @RequestParam Integer role) {
        userAdminService.addRole(id, role);
        return Result.success();
    }

    @Operation(summary = "移除角色")
    @DeleteMapping("/{id}/roles/{role}")
    public Result<Void> removeRole(@PathVariable Long id, @PathVariable Integer role) {
        userAdminService.removeRole(id, role);
        return Result.success();
    }

    @Operation(summary = "设置默认角色")
    @PutMapping("/{id}/default-role")
    public Result<Void> defaultRole(@PathVariable Long id, @RequestParam Integer role) {
        userAdminService.setDefaultRole(id, role);
        return Result.success();
    }

    @Operation(summary = "修改登录手机号")
    @PutMapping("/{id}/phone")
    public Result<Void> phone(@PathVariable Long id, @RequestParam String phone) {
        userAdminService.updatePhone(id, phone);
        return Result.success();
    }

    @Operation(summary = "重置密码")
    @PutMapping("/{id}/password")
    public Result<Void> password(@PathVariable Long id, @RequestParam String password) {
        userAdminService.resetPassword(id, password);
        return Result.success();
    }
}
