package com.visitor.server.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.visitor.common.core.PageResult;
import com.visitor.common.enums.RoleEnum;
import com.visitor.common.exception.BizException;
import com.visitor.domain.dto.UserCreateDTO;
import com.visitor.domain.entity.ApprovalTask;
import com.visitor.domain.entity.Staff;
import com.visitor.domain.entity.User;
import com.visitor.domain.entity.UserRole;
import com.visitor.server.mapper.ApprovalTaskMapper;
import com.visitor.server.mapper.StaffMapper;
import com.visitor.server.mapper.UserMapper;
import com.visitor.server.mapper.UserRoleMapper;
import com.visitor.server.service.UserAdminService;
import com.visitor.workflow.ApprovalStatuses;
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
    private static final String SOURCE_IDENTITY = "IDENTITY";
    private static final String SOURCE_MANUAL = "MANUAL";

    private final UserMapper userMapper;
    private final UserRoleMapper userRoleMapper;
    private final ApprovalTaskMapper taskMapper;
    private final PasswordEncoder passwordEncoder;
    private final StaffMapper staffMapper;

    public UserAdminServiceImpl(UserMapper userMapper, UserRoleMapper userRoleMapper,
                                ApprovalTaskMapper taskMapper, PasswordEncoder passwordEncoder,
                                StaffMapper staffMapper) {
        this.userMapper = userMapper;
        this.userRoleMapper = userRoleMapper;
        this.taskMapper = taskMapper;
        this.passwordEncoder = passwordEncoder;
        this.staffMapper = staffMapper;
    }

    @Override
    public PageResult<Map<String, Object>> page(String keyword, Integer role, int page, int size) {
        int safePage = Math.max(page, 1);
        int safeSize = Math.min(Math.max(size, 1), 100);
        if (role != null && RoleEnum.of(role) == RoleEnum.SYSTEM_ADMIN) {
            return PageResult.of(0, safePage, safeSize, List.of());
        }
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
        List<Long> superAdminIds = userRoleMapper.selectList(new LambdaQueryWrapper<UserRole>()
                        .eq(UserRole::getRole, RoleEnum.SYSTEM_ADMIN))
                .stream().map(UserRole::getUserId).toList();
        Page<User> result = userMapper.selectPage(new Page<>(safePage, safeSize),
                new LambdaQueryWrapper<User>()
                        .and(keyword != null && !keyword.isBlank(), w -> w
                                .like(User::getName, keyword)
                                .or().like(User::getPhone, keyword)
                                .or().like(User::getPersonNo, keyword))
                        .in(userIds != null, User::getId, userIds)
                        .notIn(!superAdminIds.isEmpty(), User::getId, superAdminIds)
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
        userRole.setSource(SOURCE_MANUAL);
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
        if (status == 0) {
            assertNotSuperAdmin(userId);
            assertNoPending(userId);
        }
        user.setStatus(status);
        user.setUpdatedAt(LocalDateTime.now());
        userMapper.updateById(user);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void addRole(Long userId, Integer roleCode) {
        User user = requireUser(userId);
        RoleEnum role = RoleEnum.of(roleCode);
        if (role == null || role == RoleEnum.SYSTEM_ADMIN) {
            throw new BizException(400, "不允许分配该角色");
        }
        if (role == RoleEnum.COUNSELOR) {
            throw new BizException(400, "辅导员角色只能通过辅导员管理授予");
        }
        UserRole existing = userRoleMapper.selectOne(new LambdaQueryWrapper<UserRole>()
                .eq(UserRole::getUserId, user.getId())
                .eq(UserRole::getRole, role)
                .last("LIMIT 1"));
        if (existing != null) {
            existing.setStatus(1);
            existing.setUpdatedAt(LocalDateTime.now());
            userRoleMapper.updateById(existing);
            return;
        }
        UserRole userRole = new UserRole();
        userRole.setUserId(user.getId());
        userRole.setRole(role);
        userRole.setSchoolId(1L);
        userRole.setIsDefault(0);
        userRole.setSource(SOURCE_MANUAL);
        userRole.setStatus(1);
        userRole.setCreatedAt(LocalDateTime.now());
        userRole.setUpdatedAt(LocalDateTime.now());
        userRoleMapper.insert(userRole);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void removeRole(Long userId, Integer roleCode) {
        requireUser(userId);
        RoleEnum role = RoleEnum.of(roleCode);
        if (role == null) {
            throw new BizException(400, "角色不合法");
        }
        if (role == RoleEnum.SYSTEM_ADMIN) {
            throw new BizException(400, "系统管理员角色不允许移除");
        }
        if (role == RoleEnum.COUNSELOR) {
            throw new BizException(400, "辅导员角色只能通过辅导员管理移除");
        }
        UserRole existing = userRoleMapper.selectOne(new LambdaQueryWrapper<UserRole>()
                .eq(UserRole::getUserId, userId)
                .eq(UserRole::getRole, role)
                .last("LIMIT 1"));
        if (existing == null) {
            throw new BizException(404, "角色不存在");
        }
        if (SOURCE_IDENTITY.equals(existing.getSource())) {
            throw new BizException(400, "该角色由人员类型决定，请修改人员类型或停用人员");
        }
        Long total = userRoleMapper.selectCount(new LambdaQueryWrapper<UserRole>()
                .eq(UserRole::getUserId, userId));
        if (total != null && total <= 1) {
            throw new BizException(400, "账号至少保留一个角色");
        }
        userRoleMapper.deleteById(existing.getId());
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void setDefaultRole(Long userId, Integer roleCode) {
        requireUser(userId);
        RoleEnum role = RoleEnum.of(roleCode);
        if (role == null) {
            throw new BizException(400, "角色不合法");
        }
        UserRole target = userRoleMapper.selectOne(new LambdaQueryWrapper<UserRole>()
                .eq(UserRole::getUserId, userId)
                .eq(UserRole::getRole, role)
                .eq(UserRole::getStatus, 1)
                .last("LIMIT 1"));
        if (target == null) {
            throw new BizException(400, "账号没有该角色");
        }
        for (UserRole item : userRoleMapper.selectList(new LambdaQueryWrapper<UserRole>()
                .eq(UserRole::getUserId, userId)
                .eq(UserRole::getIsDefault, 1))) {
            item.setIsDefault(0);
            item.setUpdatedAt(LocalDateTime.now());
            userRoleMapper.updateById(item);
        }
        target.setIsDefault(1);
        target.setUpdatedAt(LocalDateTime.now());
        userRoleMapper.updateById(target);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void updatePhone(Long userId, String phone) {
        User user = requireUser(userId);
        if (phone == null || phone.isBlank()) {
            throw new BizException(400, "手机号不能为空");
        }
        Staff linked = user.getPersonNo() == null ? null : staffMapper.selectOne(
                new LambdaQueryWrapper<Staff>()
                        .eq(Staff::getPersonNo, user.getPersonNo())
                        .eq(Staff::getIsDeleted, 0)
                        .last("LIMIT 1"));
        if (linked != null) {
            throw new BizException(400, "该账号已关联人员，请到人员管理修改手机号");
        }
        String value = phone.trim();
        Long exists = userMapper.selectCount(new LambdaQueryWrapper<User>()
                .eq(User::getPhone, value)
                .ne(User::getId, userId));
        if (exists != null && exists > 0) {
            throw new BizException(400, "手机号已存在");
        }
        user.setPhone(value);
        user.setUpdatedAt(LocalDateTime.now());
        userMapper.updateById(user);
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

    private void assertNotSuperAdmin(Long userId) {
        Long count = userRoleMapper.selectCount(new LambdaQueryWrapper<UserRole>()
                .eq(UserRole::getUserId, userId)
                .eq(UserRole::getRole, RoleEnum.SYSTEM_ADMIN));
        if (count != null && count > 0) {
            throw new BizException(400, "系统管理员账号不允许停用");
        }
    }

    private void assertNoPending(Long userId) {
        Long pending = taskMapper.selectCount(new LambdaQueryWrapper<ApprovalTask>()
                .eq(ApprovalTask::getAssigneeId, userId)
                .eq(ApprovalTask::getStatus, ApprovalStatuses.PENDING));
        if (pending != null && pending > 0) {
            throw new BizException(400, "还有 " + pending + " 条待审批，请先转交");
        }
    }

    private User requireUser(Long userId) {
        User user = userMapper.selectById(userId);
        if (user == null) {
            throw new BizException(404, "用户不存在");
        }
        return user;
    }

    private Map<String, Object> toView(User user) {
        List<UserRole> roleRows = userRoleMapper.selectList(new LambdaQueryWrapper<UserRole>()
                .eq(UserRole::getUserId, user.getId()));
        Map<String, Object> view = new LinkedHashMap<>();
        view.put("id", user.getId());
        view.put("phone", user.getPhone());
        view.put("personNo", user.getPersonNo());
        view.put("name", user.getName());
        view.put("status", user.getStatus());
        view.put("roles", roleRows.stream()
                .filter(item -> Integer.valueOf(1).equals(item.getStatus()))
                .map(item -> item.getRole().getCode()).toList());
        view.put("roleDetails", roleRows.stream().map(item -> {
            Map<String, Object> role = new LinkedHashMap<>();
            role.put("code", item.getRole().getCode());
            role.put("name", item.getRole().getDescription());
            role.put("source", item.getSource() == null ? SOURCE_MANUAL : item.getSource());
            role.put("isDefault", item.getIsDefault());
            role.put("status", item.getStatus());
            return role;
        }).toList());
        return view;
    }
}
