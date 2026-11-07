package com.visitor.server.controller.admin;

import com.visitor.common.core.Result;
import com.visitor.domain.entity.ParentSlot;
import com.visitor.server.service.ParentSlotService;
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

@Tag(name = "家长固定时段")
@RestController
@RequestMapping("/web/admin/parent-slots")
public class ParentSlotController {

    private final ParentSlotService parentSlotService;

    public ParentSlotController(ParentSlotService parentSlotService) {
        this.parentSlotService = parentSlotService;
    }

    @Operation(summary = "时段列表")
    @GetMapping
    public Result<List<ParentSlot>> list(@RequestParam(required = false) String visitorPhone) {
        return Result.success(parentSlotService.list(visitorPhone));
    }

    @Operation(summary = "新增时段")
    @PostMapping
    public Result<ParentSlot> create(@RequestBody ParentSlot body) {
        return Result.success(parentSlotService.create(body));
    }

    @Operation(summary = "修改时段")
    @PutMapping("/{id}")
    public Result<ParentSlot> update(@PathVariable Long id, @RequestBody ParentSlot body) {
        return Result.success(parentSlotService.update(id, body));
    }

    @Operation(summary = "删除时段")
    @DeleteMapping("/{id}")
    public Result<Void> delete(@PathVariable Long id) {
        parentSlotService.delete(id);
        return Result.success();
    }
}
