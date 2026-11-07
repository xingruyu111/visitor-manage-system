package com.visitor.server.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.visitor.common.core.PageResult;
import com.visitor.common.enums.RoleEnum;
import com.visitor.common.exception.BizException;
import com.visitor.domain.dto.UserCreateDTO;
import com.visitor.domain.entity.User;
import com.visitor.domain.entity.UserRole;
import com.visitor.server.mapper.UserMapper;
import com.visitor.server.mapper.UserRoleMapper;
import com.visitor.server.service.UserAdminService;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Service
public class UserAdminServiceImpl implements UserAdminService {

    private static final String DEFAULT_PASSWORD = "123456";

    private final UserMapper userMapper;
    private final UserRoleMapper userRoleMapper;
    private final PasswordEncoder passwordEncoder;

    public UserAdminServiceImpl(UserMapper userMapper, UserRoleMapper userRoleMapper,
                                PasswordEncoder passwordEncoder) {
        this.userMapper = userMapper;
        this.userRoleMapper = userRoleMapper;
        this.passwordEncoder = passwordEncoder;
    }

    @Override
    public PageResult<Map<String, Object>> page(String keyword, Integer role, int page, int size) {
        int safePage = Math.max(page, 1);
        int safeSize = Math.min(Math.max(size, 1), 100);
        List<Long> userIds = null;
        if (role != null) {
            RoleEnum roleEnum = RoleEnum.of(role);
            if (roleEnum == null) {
                throw new BizException(400, "角色不合法");
            }
            userIds = userRoleMapper.selectList(new LambdaQueryWrapper<UserRole>()
                            .eq(UserRole::getRole, roleEnum)
                            .eq(UserRole::getStatus, 1))
                    .stream().map(UserRole::getUserId).toList();
            if (userIds.isEmpty()) {
                return PageResult.of(0, safePage, safeSize, List.of());
            }
        }
        Page<User> result = userMapper.selectPage(new Page<>(safePage, safeSize),
                new LambdaQueryWrapper<User>()
                        .like(keyword != null && !keyword.isBlank(), User::getName, keyword)
                        .or(keyword != null && !keyword.isBlank(), w -> w.like(User::getPhone, keyword))
                        .in(userIds != null, User::getId, userIds)
                        .orderByDesc(User::getId));
        List<Map<String, Object>> records = new ArrayList<>();
        for (User user : result.getRecords()) {
            records.add(toView(user));
        }
        return PageResult.of(result.getTotal(), safePage, safeSize, records);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Map<String, Object> create(UserCreateDTO dto) {
        RoleEnum role = RoleEnum.of(dto.getRole());
        if (role == null || role == RoleEnum.SYSTEM_ADMIN) {
            throw new BizException(400, "不允许创建该角色");
        }
        Long exists = userMapper.selectCount(new LambdaQueryWrapper<User>().eq(User::getPhone, dto.getPhone()));
        if (exists != null && exists > 0) {
            throw new BizException(400, "手机号已存在");
        }
        User user = new User();
        user.setPhone(dto.getPhone().trim());
        user.setName(dto.getName().trim());
        user.setPassword(passwordEncoder.encode(
                dto.getPassword() == null || dto.getPassword().isBlank() ? DEFAULT_PASSWORD : dto.getPassword()));
        user.setStatus(1);
        user.setCreatedAt(LocalDateTime.now());
        user.setUpdatedAt(LocalDateTime.now());
        userMapper.insert(user);

        UserRole userRole = new UserRole();
        userRole.setUserId(user.getId());
        userRole.setRole(role);
        userRole.setSchoolId(1L);
        userRole.setIsDefault(1);
        userRole.setStatus(1);
        userRole.setCreatedAt(LocalDateTime.now());
        userRole.setUpdatedAt(LocalDateTime.now());
        userRoleMapper.insert(userRole);
        return toView(user);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void updateStatus(Long userId, Integer status) {
        User user = requireUser(userId);
        if (status == null || (status != 0 && status != 1)) {
            throw new BizException(400, "状态不合法");
        }
        user.setStatus(status);
        user.setUpdatedAt(LocalDateTime.now());
        userMapper.updateById(user);
        for (UserRole role : userRoleMapper.selectList(new LambdaQueryWrapper<UserRole>()
                .eq(UserRole::getUserId, userId))) {
            role.setStatus(status);
            role.setUpdatedAt(LocalDateTime.now());
            userRoleMapper.updateById(role);
        }
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void assignRole(Long userId, Integer roleCode) {
        requireUser(userId);
        RoleEnum role = RoleEnum.of(roleCode);
        if (role == null || role == RoleEnum.SYSTEM_ADMIN) {
            throw new BizException(400, "不允许分配该角色");
        }
        UserRole existing = userRoleMapper.selectOne(new LambdaQueryWrapper<UserRole>()
                .eq(UserRole::getUserId, userId).last("LIMIT 1"));
        if (existing == null) {
            UserRole userRole = new UserRole();
            userRole.setUserId(userId);
            userRole.setRole(role);
            userRole.setSchoolId(1L);
            userRole.setIsDefault(1);
            userRole.setStatus(1);
            userRole.setCreatedAt(LocalDateTime.now());
            userRole.setUpdatedAt(LocalDateTime.now());
            userRoleMapper.insert(userRole);
            return;
        }
        existing.setRole(role);
        existing.setStatus(1);
        existing.setUpdatedAt(LocalDateTime.now());
        userRoleMapper.updateById(existing);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void resetPassword(Long userId, String password) {
        User user = requireUser(userId);
        if (password == null || password.length() < 6) {
            throw new BizException(400, "密码至少 6 位");
        }
        user.setPassword(passwordEncoder.encode(password));
        user.setUpdatedAt(LocalDateTime.now());
        userMapper.updateById(user);
    }

    private User requireUser(Long userId) {
        User user = userMapper.selectById(userId);
        if (user == null) {
            throw new BizException(404, "用户不存在");
        }
        return user;
    }

    private Map<String, Object> toView(User user) {
        Map<String, Object> view = new LinkedHashMap<>();
        view.put("id", user.getId());
        view.put("phone", user.getPhone());
        view.put("name", user.getName());
        view.put("status", user.getStatus());
        view.put("roles", userRoleMapper.selectList(new LambdaQueryWrapper<UserRole>()
                        .eq(UserRole::getUserId, user.getId()))
                .stream().map(item -> item.getRole().getCode()).toList());
        return view;
    }
}
