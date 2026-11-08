package com.visitor.server.controller;

import com.visitor.common.core.Result;
import com.visitor.server.service.WechatPhoneService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@Tag(name = "微信手机号")
@RestController
@RequestMapping("/auth")
public class WechatPhoneController {

    private final WechatPhoneService wechatPhoneService;

    public WechatPhoneController(WechatPhoneService wechatPhoneService) {
        this.wechatPhoneService = wechatPhoneService;
    }

    @Operation(summary = "小程序 phoneCode 换手机号（MOCK）")
    @PostMapping("/wechat/phone")
    public Result<Map<String, Object>> phone(@RequestBody Map<String, String> body) {
        return Result.success(wechatPhoneService.getPhone(body.get("phoneCode")));
    }
}
