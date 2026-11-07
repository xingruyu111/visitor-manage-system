package com.visitor.server.service.impl;

import com.visitor.domain.entity.MpConfig;
import com.visitor.domain.entity.User;
import com.visitor.server.mapper.UserMapper;
import com.visitor.server.service.MpConfigService;
import lombok.extern.slf4j.Slf4j;
import me.chanjar.weixin.mp.api.WxMpService;
import me.chanjar.weixin.mp.api.impl.WxMpServiceImpl;
import me.chanjar.weixin.mp.bean.template.WxMpTemplateData;
import me.chanjar.weixin.mp.bean.template.WxMpTemplateMessage;
import me.chanjar.weixin.mp.config.impl.WxMpDefaultConfigImpl;
import org.springframework.stereotype.Component;

/**
 * 服务号模板消息：配置齐全且用户已绑定 openid 时发送，失败只记日志。
 */
@Slf4j
@Component
public class WxMpNotifier {

    private final MpConfigService mpConfigService;
    private final UserMapper userMapper;

    public WxMpNotifier(MpConfigService mpConfigService, UserMapper userMapper) {
        this.mpConfigService = mpConfigService;
        this.userMapper = userMapper;
    }

    public void sendAuditResult(Long userId, String visitorName, String result) {
        MpConfig config = mpConfigService.get();
        if (config == null || isBlank(config.getAppId()) || isBlank(config.getAuditResultTemplateId())) {
            return;
        }
        User user = userMapper.selectById(userId);
        if (user == null || isBlank(user.getMpOpenid())) {
            return;
        }
        try {
            WxMpDefaultConfigImpl storage = new WxMpDefaultConfigImpl();
            storage.setAppId(config.getAppId());
            storage.setSecret(config.getSecret());
            WxMpService service = new WxMpServiceImpl();
            service.setWxMpConfigStorage(storage);

            WxMpTemplateMessage message = WxMpTemplateMessage.builder()
                    .toUser(user.getMpOpenid())
                    .templateId(config.getAuditResultTemplateId())
                    .build();
            message.addData(new WxMpTemplateData("thing2", truncate(visitorName)));
            message.addData(new WxMpTemplateData("thing1", truncate(result)));
            service.getTemplateMsgService().sendTemplateMsg(message);
            log.info("服务号审核结果通知已发送: userId={}", userId);
        } catch (Exception e) {
            log.warn("服务号通知发送失败（不影响主流程）: userId={}, error={}", userId, e.getMessage());
        }
    }

    private boolean isBlank(String value) {
        return value == null || value.isBlank();
    }

    private String truncate(String value) {
        if (value == null) {
            return "";
        }
        return value.length() > 20 ? value.substring(0, 20) : value;
    }
}
