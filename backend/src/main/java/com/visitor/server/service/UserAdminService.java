package com.visitor.server.service;

import com.visitor.common.core.PageResult;
import com.visitor.domain.dto.UserCreateDTO;

import java.util.Map;

public interface UserAdminService {

    PageResult<Map<String, Object>> page(String keyword, Integer role, int page, int size);

    Map<String, Object> create(UserCreateDTO dto);

    void updateStatus(Long userId, Integer status);

    void addRole(Long userId, Integer role);

    void removeRole(Long userId, Integer role);

    void setDefaultRole(Long userId, Integer role);

    void updatePhone(Long userId, String phone);

    void resetPassword(Long userId, String password);
}
