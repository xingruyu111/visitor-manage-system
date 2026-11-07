package com.visitor.server.service;

import com.visitor.common.core.PageResult;
import com.visitor.domain.dto.UserCreateDTO;

import java.util.Map;

public interface UserAdminService {

    PageResult<Map<String, Object>> page(String keyword, Integer role, int page, int size);

    Map<String, Object> create(UserCreateDTO dto);

    void updateStatus(Long userId, Integer status);

    void assignRole(Long userId, Integer role);

    void resetPassword(Long userId, String password);
}
