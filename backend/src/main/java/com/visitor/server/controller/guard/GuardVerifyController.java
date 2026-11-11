package com.visitor.server.controller.guard;

import com.visitor.common.core.PageResult;
import com.visitor.common.core.Result;
import com.visitor.common.enums.RoleEnum;
import com.visitor.common.exception.BizException;
import com.visitor.domain.entity.VisitLog;
import com.visitor.security.SchoolScopes;
import com.visitor.security.UserContext;
import com.visitor.server.service.VerifyService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@Tag(name = "门卫核验")
@RestController
@RequestMapping("/web/guard/verify")
public class GuardVerifyController {

    private final VerifyService verifyService;

    public GuardVerifyController(VerifyService verifyService) {
        this.verifyService = verifyService;
    }

    @Operation(summary = "扫码核验（入校/离校）")
    @PostMapping
    public Result<Map<String, Object>> verify(@RequestParam String token) {
        RoleEnum role = UserContext.getRole();
        if (role != RoleEnum.GUARD && role != RoleEnum.SECURITY_ADMIN && role != RoleEnum.SYSTEM_ADMIN) {
            throw new BizException(403, "门卫才能扫码核验");
        }
        return Result.success(verifyService.verify(UserContext.getUserId(), token));
    }

    @Operation(summary = "通行记录分页")
    @GetMapping("/records")
    public Result<PageResult<VisitLog>> records(@RequestParam(required = false) Long schoolId,
                                                @RequestParam(defaultValue = "1") int page,
                                                @RequestParam(defaultValue = "20") int size) {
        return Result.success(verifyService.records(SchoolScopes.resolve(schoolId), page, size));
    }
}
