package com.visitor.server.controller;

import com.visitor.common.core.Result;
import com.visitor.domain.dto.WechatLoginDTO;
import com.visitor.server.service.WechatLoginService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@Tag(name = "微信授权登录")
@RestController
@RequestMapping("/auth")
public class WechatAuthController {

    private final WechatLoginService wechatLoginService;

    public WechatAuthController(WechatLoginService wechatLoginService) {
        this.wechatLoginService = wechatLoginService;
    }

    @Operation(summary = "公众号网页授权登录（code + 手机号绑定）")
    @PostMapping("/wechat")
    public Result<Map<String, Object>> login(@Valid @RequestBody WechatLoginDTO dto) {
        return Result.success(wechatLoginService.login(dto));
    }
}
