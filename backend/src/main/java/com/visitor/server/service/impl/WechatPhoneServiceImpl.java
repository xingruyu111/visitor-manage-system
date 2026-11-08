package com.visitor.server.service.impl;

import com.visitor.common.exception.BizException;
import com.visitor.domain.entity.MpConfig;
import com.visitor.server.service.MpConfigService;
import com.visitor.server.service.WechatPhoneService;
import org.springframework.stereotype.Service;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * 微信手机号获取：MOCK 模式由 phoneCode 生成固定手机号，REST 需小程序凭证（未实现）。
 */
@Service
public class WechatPhoneServiceImpl implements WechatPhoneService {

    private final MpConfigService mpConfigService;

    public WechatPhoneServiceImpl(MpConfigService mpConfigService) {
        this.mpConfigService = mpConfigService;
    }

    @Override
    public Map<String, Object> getPhone(String phoneCode) {
        MpConfig config = mpConfigService.get();
        if (config == null || config.getAppId() == null || config.getAppId().isBlank()) {
            throw new BizException(400, "微信登录未配置公众号/小程序");
        }
        if (!"MOCK".equalsIgnoreCase(config.getAuthMode())) {
            throw new BizException(400, "获取手机号需小程序凭证，请配置后切换 REST 实现");
        }
        if (phoneCode == null || phoneCode.isBlank()) {
            throw new BizException(400, "phoneCode 不能为空");
        }
        String phone = mockPhone(phoneCode);
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("phone", phone);
        result.put("mock", true);
        return result;
    }

    private String mockPhone(String code) {
        int hash = Math.abs(code.hashCode());
        return String.format("139%08d", hash % 100_000_000);
    }
}
