package com.visitor.server.controller;

import com.visitor.common.core.Result;
import com.visitor.security.CaptchaService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "验证码")
@RestController
@RequestMapping("/auth")
public class CaptchaController {

    private final CaptchaService captchaService;

    public CaptchaController(CaptchaService captchaService) {
        this.captchaService = captchaService;
    }

    @Operation(summary = "获取图形验证码（未开启时返回 enabled=false）")
    @GetMapping("/captcha")
    public Result<Object> captcha() {
        if (!captchaService.isEnabled()) {
            return Result.success(java.util.Map.of("enabled", false));
        }
        CaptchaService.Captcha captcha = captchaService.generate();
        return Result.success(java.util.Map.of(
                "enabled", true,
                "captchaId", captcha.captchaId(),
                "image", captcha.image()));
    }
}
