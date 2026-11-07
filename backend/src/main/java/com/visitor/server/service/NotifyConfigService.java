package com.visitor.server.service;

import com.visitor.domain.entity.NotifyConfig;

import java.util.List;

public interface NotifyConfigService {

    List<NotifyConfig> list();

    NotifyConfig update(String notifyType, Integer enabled, String channels);

    /** 未配置时默认开启。 */
    boolean isEnabled(String notifyType);
}
