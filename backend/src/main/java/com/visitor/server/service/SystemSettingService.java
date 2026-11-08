package com.visitor.server.service;

import com.visitor.domain.entity.SystemSetting;

import java.util.List;

public interface SystemSettingService {

    List<SystemSetting> list();

    SystemSetting update(String key, String value);

    String get(String key, String defaultValue);

    int getInt(String key, int defaultValue);

    boolean getBool(String key, boolean defaultValue);

    List<String> getList(String key, String defaultValue);
}
