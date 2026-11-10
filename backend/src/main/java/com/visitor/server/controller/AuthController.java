package com.visitor.server.controller;

import com.visitor.common.core.Result;
import com.visitor.common.exception.BizException;
import com.visitor.domain.dto.LoginDTO;
import com.visitor.domain.vo.LoginVO;
import com.visitor.domain.vo.UserInfoVO;
import com.visitor.integration.IntegrationFactory;
import com.visitor.integration.sso.IdentityProvider;
import com.visitor.security.UserContext;
import com.visitor.security.IpBanService;
import com.visitor.security.CaptchaService;
import com.visitor.server.service.IpBlacklistService;
import com.visitor.server.service.AuthService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;
import java.util.UUID;

@Tag(name = "认证")
@RestController
@RequestMapping("/auth")
public class AuthController {

    private final AuthService authService;
    private final IpBanService ipBanService;
    private final CaptchaService captchaService;
    private final IpBlacklistService ipBlacklistService;
    private final IntegrationFactory integrationFactory;

    public AuthController(AuthService authService, IpBanService ipBanService, IpBlacklistService ipBlacklistService, CaptchaService captchaService, IntegrationFactory integrationFactory) {
        this.authService = authService;
        this.ipBanService = ipBanService;
        this.captchaService = captchaService;
        this.ipBlacklistService = ipBlacklistService;
        this.integrationFactory = integrationFactory;
    }

    @Operation(summary = "账号密码登录")
    @PostMapping("/login")
    public Result<LoginVO> login(@Valid @RequestBody LoginDTO dto, HttpServletRequest request) {
        ipBlacklistService.assertAllowed(clientIp(request));
        captchaService.validate(dto.getCaptchaId(), dto.getCaptchaCode());
        ipBanService.assertAllowed();
        try {
            LoginVO loginVO = authService.login(dto.getPhone(), dto.getPassword(), clientIp(request));
            ipBanService.clear();
            return Result.success(loginVO);
        } catch (BizException e) {
            ipBanService.recordFailure();
            throw e;
        }
    }

    @Operation(summary = "H5 登录：未注册手机号自动创建访客账号")
    @PostMapping("/visitor-login")
    public Result<LoginVO> visitorLogin(@Valid @RequestBody LoginDTO dto, HttpServletRequest request) {
        ipBlacklistService.assertAllowed(clientIp(request));
        captchaService.validate(dto.getCaptchaId(), dto.getCaptchaCode());
        ipBanService.assertAllowed();
        try {
            LoginVO loginVO = authService.loginOrRegisterVisitor(dto.getPhone(), dto.getPassword(), clientIp(request));
            ipBanService.clear();
            return Result.success(loginVO);
        } catch (BizException e) {
            ipBanService.recordFailure();
            throw e;
        }
    }

    @Operation(summary = "退出登录（令牌加入黑名单）")
    @PostMapping("/logout")
    public Result<Void> logout(@RequestHeader(value = "Authorization", required = false) String authorization) {
        if (authorization != null && authorization.startsWith("Bearer ")) {
            authService.logout(authorization.substring(7));
        }
        return Result.success();
    }

    @Operation(summary = "当前登录用户")
    @GetMapping("/me")
    public Result<UserInfoVO> me() {
        return Result.success(authService.me(UserContext.getUserId()));
    }

    @Operation(summary = "获取统一认证跳转地址")
    @GetMapping("/sso/authorize")
    public Result<Map<String, String>> authorize(@RequestParam String redirectUri) {
        IdentityProvider provider = integrationFactory.identity();
        if (!provider.isConfigured()) {
            throw new BizException(400, "统一身份认证未配置");
        }
        String url = provider.buildAuthorizeUrl(redirectUri, UUID.randomUUID().toString());
        return Result.success(Map.of("url", url));
    }

    @Operation(summary = "统一认证回调：换取本地令牌")
    @GetMapping("/sso/callback")
    public Result<LoginVO> ssoCallback(@RequestParam String code, @RequestParam String redirectUri) {
        return Result.success(authService.ssoLogin(code, redirectUri));
    }

    private String clientIp(HttpServletRequest request) {
        String forwarded = request.getHeader("X-Forwarded-For");
        if (forwarded != null && !forwarded.isBlank()) {
            return forwarded.split(",")[0].trim();
        }
        return request.getRemoteAddr();
    }
}
