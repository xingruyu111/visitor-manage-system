package com.visitor.server.service;

import java.util.Map;

public interface WechatPhoneService {

    Map<String, Object> getPhone(String phoneCode);
}
