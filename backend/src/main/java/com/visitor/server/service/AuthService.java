package com.visitor.server.service;

import com.visitor.domain.vo.LoginVO;
import com.visitor.domain.vo.UserInfoVO;

public interface AuthService {

    LoginVO login(String phone, String password, String ip);

    LoginVO ssoLogin(String code, String redirectUri);

    UserInfoVO me(Long userId);

    void logout(String token);
}
