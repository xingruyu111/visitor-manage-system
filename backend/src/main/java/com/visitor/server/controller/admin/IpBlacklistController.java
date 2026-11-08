package com.visitor.server.controller.admin;

import com.visitor.common.core.PageResult;
import com.visitor.common.core.Result;
import com.visitor.domain.entity.IpBlacklist;
import com.visitor.security.UserContext;
import com.visitor.server.service.IpBlacklistService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDateTime;

@Tag(name = "IP 黑名单")
@RestController
@RequestMapping("/web/admin/ip-blacklist")
public class IpBlacklistController {

    private final IpBlacklistService ipBlacklistService;

    public IpBlacklistController(IpBlacklistService ipBlacklistService) {
        this.ipBlacklistService = ipBlacklistService;
    }

    @Operation(summary = "IP 黑名单分页")
    @GetMapping
    public Result<PageResult<IpBlacklist>> page(@RequestParam(required = false) String keyword,
                                                @RequestParam(defaultValue = "1") int page,
                                                @RequestParam(defaultValue = "20") int size) {
        return Result.success(ipBlacklistService.page(keyword, page, size));
    }

    @Operation(summary = "新增 IP 黑名单")
    @PostMapping
    public Result<IpBlacklist> add(
            @RequestParam String ip,
            @RequestParam(required = false) String reason,
            @RequestParam(required = false)
            @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss") LocalDateTime expireAt) {
        return Result.success(ipBlacklistService.add(ip, reason, expireAt, UserContext.getUserId()));
    }

    @Operation(summary = "移除 IP 黑名单")
    @DeleteMapping("/{id}")
    public Result<Void> remove(@PathVariable Long id) {
        ipBlacklistService.remove(id);
        return Result.success();
    }
}
