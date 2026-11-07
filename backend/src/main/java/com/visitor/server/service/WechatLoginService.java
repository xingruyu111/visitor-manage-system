package com.visitor.server.service;

import com.visitor.domain.dto.WechatLoginDTO;

import java.util.Map;

public interface WechatLoginService {

    Map<String, Object> login(WechatLoginDTO dto);
}
