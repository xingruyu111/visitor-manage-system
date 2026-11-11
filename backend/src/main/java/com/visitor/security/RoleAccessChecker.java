package com.visitor.security;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.visitor.common.enums.RoleEnum;
import com.visitor.domain.entity.UserRole;
import com.visitor.server.mapper.UserRoleMapper;
import org.springframework.stereotype.Component;

/**
 * 按账号实际拥有的角色判断权限，不依赖登录令牌里的“当前角色”。
 * 这样账号同时拥有人工添加的门卫/保安角色时，也能使用对应功能。
 */
@Component
public class RoleAccessChecker {

    private final UserRoleMapper userRoleMapper;

    public RoleAccessChecker(UserRoleMapper userRoleMapper) {
        this.userRoleMapper = userRoleMapper;
    }

    public boolean hasAny(RoleEnum... roles) {
        Long userId = UserContext.getUserId();
        if (userId == null || roles == null || roles.length == 0) {
            return false;
        }
        Long count = userRoleMapper.selectCount(new LambdaQueryWrapper<UserRole>()
                .eq(UserRole::getUserId, userId)
                .in(UserRole::getRole, java.util.List.of(roles))
                .eq(UserRole::getStatus, 1));
        return count != null && count > 0;
    }
}
