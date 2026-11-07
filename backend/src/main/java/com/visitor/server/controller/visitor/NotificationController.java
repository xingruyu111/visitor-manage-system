package com.visitor.server.controller.visitor;

import com.visitor.common.core.PageResult;
import com.visitor.common.core.Result;
import com.visitor.domain.entity.Notification;
import com.visitor.security.UserContext;
import com.visitor.server.service.NotificationService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@Tag(name = "站内通知")
@RestController
@RequestMapping("/web/notifications")
public class NotificationController {

    private final NotificationService notificationService;

    public NotificationController(NotificationService notificationService) {
        this.notificationService = notificationService;
    }

    @Operation(summary = "我的通知")
    @GetMapping
    public Result<PageResult<Notification>> my(@RequestParam(defaultValue = "1") int page,
                                               @RequestParam(defaultValue = "20") int size) {
        return Result.success(notificationService.my(UserContext.getUserId(), page, size));
    }

    @Operation(summary = "未读数")
    @GetMapping("/unread-count")
    public Result<Map<String, Object>> unread() {
        return Result.success(Map.of("count", notificationService.unread(UserContext.getUserId())));
    }

    @Operation(summary = "标记已读")
    @PostMapping("/{id}/read")
    public Result<Void> read(@PathVariable Long id) {
        notificationService.markRead(UserContext.getUserId(), id);
        return Result.success();
    }

    @Operation(summary = "全部已读")
    @PostMapping("/read-all")
    public Result<Map<String, Object>> readAll() {
        return Result.success(Map.of("updated", notificationService.markAllRead(UserContext.getUserId())));
    }
}
