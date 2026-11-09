package com.visitor.server.controller.admin;

import com.visitor.common.core.Result;
import com.visitor.domain.entity.Org;
import com.visitor.server.service.OrgService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

@Tag(name = "组织架构")
@RestController
@RequestMapping("/web/admin/orgs")
public class OrgController {

    private final OrgService orgService;

    public OrgController(OrgService orgService) {
        this.orgService = orgService;
    }

    @Operation(summary = "组织树")
    @GetMapping("/tree")
    public Result<List<Map<String, Object>>> tree() {
        return Result.success(orgService.tree());
    }

    @Operation(summary = "新增组织节点")
    @PostMapping
    public Result<Org> create(@RequestParam(required = false) Long parentId,
                              @RequestParam String name,
                              @RequestParam(required = false) Integer sortOrder) {
        return Result.success(orgService.create(parentId, name, sortOrder));
    }

    @Operation(summary = "修改组织节点")
    @PutMapping("/{id}")
    public Result<Org> update(@PathVariable Long id,
                              @RequestParam(required = false) Long parentId,
                              @RequestParam(required = false) String name,
                              @RequestParam(required = false) Integer sortOrder) {
        return Result.success(orgService.update(id, parentId, name, sortOrder));
    }

    @Operation(summary = "删除组织节点")
    @DeleteMapping("/{id}")
    public Result<Void> delete(@PathVariable Long id) {
        orgService.delete(id);
        return Result.success();
    }
}
