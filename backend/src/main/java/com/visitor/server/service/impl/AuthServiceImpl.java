package com.visitor.server.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.visitor.common.enums.RoleEnum;
import com.visitor.common.exception.BizException;
import com.visitor.domain.entity.User;
import com.visitor.domain.entity.Staff;
import com.visitor.domain.entity.UserRole;
import com.visitor.common.enums.PersonTypeEnum;
import com.visitor.domain.vo.LoginVO;
import com.visitor.domain.vo.RoleVO;
import com.visitor.domain.vo.UserInfoVO;
import com.visitor.integration.IntegrationFactory;
import com.visitor.integration.sso.IdentityProvider;
import com.visitor.security.JwtUtil;
import com.visitor.security.LoginAttemptService;
import com.visitor.security.TokenBlacklist;
import com.visitor.security.UserContext;
import com.visitor.server.mapper.StaffMapper;
import com.visitor.server.mapper.UserMapper;
import com.visitor.server.mapper.UserRoleMapper;
import com.visitor.server.service.AuthService;
import io.jsonwebtoken.Claims;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Date;
import java.util.List;
import java.util.UUID;

@Service
public class AuthServiceImpl implements AuthService {

    private static final Logger log = LoggerFactory.getLogger(AuthServiceImpl.class);

    private final UserMapper userMapper;
    private final UserRoleMapper userRoleMapper;
    private final PasswordEncoder passwordEncoder;
    private final JwtUtil jwtUtil;
    private final TokenBlacklist tokenBlacklist;
    private final IntegrationFactory integrationFactory;
    private final LoginAttemptService loginAttemptService;
    private final StaffMapper staffMapper;

    public AuthServiceImpl(UserMapper userMapper, UserRoleMapper userRoleMapper,
                           PasswordEncoder passwordEncoder, JwtUtil jwtUtil,
                           TokenBlacklist tokenBlacklist, IntegrationFactory integrationFactory,
                           LoginAttemptService loginAttemptService, StaffMapper staffMapper) {
        this.userMapper = userMapper;
        this.userRoleMapper = userRoleMapper;
        this.passwordEncoder = passwordEncoder;
        this.jwtUtil = jwtUtil;
        this.tokenBlacklist = tokenBlacklist;
        this.integrationFactory = integrationFactory;
        this.loginAttemptService = loginAttemptService;
        this.staffMapper = staffMapper;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public LoginVO login(String phone, String password, String ip) {
        loginAttemptService.assertAllowed(phone);

        User user = findByPhone(phone);
        if (user == null || user.getPassword() == null || !passwordEncoder.matches(password, user.getPassword())) {
            loginAttemptService.recordFailure(phone);
            throw new BizException(401, "账号或密码错误");
        }
        if (!Integer.valueOf(1).equals(user.getStatus())) {
            throw new BizException(403, "账号已停用");
        }
        syncStaffIdentity(user);
        List<UserRole> roles = activeRoles(user.getId());
        if (roles.isEmpty()) {
            throw new BizException(403, "账号未分配角色");
        }

        UserRole current = pickDefault(roles);
        String token = jwtUtil.generate(user.getId(), user.getName(),
                roles.stream().map(UserRole::getRole).toList(), current.getRole(), current.getSchoolId());

        user.setLastLoginTime(LocalDateTime.now());
        user.setLastLoginIp(ip);
        userMapper.updateById(user);

        loginAttemptService.clear(phone);
        return buildLoginVO(user, roles, current, token);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public LoginVO loginOrRegisterVisitor(String phone, String password, String ip) {
        User existing = findByPhone(phone);
        if (existing != null) {
            return login(phone, password, ip);
        }
        User visitor = new User();
        visitor.setPhone(phone);
        Staff staff = findActiveStaffByPhone(phone);
        visitor.setName(staff != null ? staff.getName() : "访客" + phone.substring(Math.max(0, phone.length() - 4)));
        visitor.setPersonNo(staff == null || personNoUsedByOther(staff.getPersonNo(), null)
                ? null : staff.getPersonNo());
        visitor.setPassword(passwordEncoder.encode(password));
        visitor.setStatus(1);
        visitor.setCreatedAt(LocalDateTime.now());
        visitor.setUpdatedAt(LocalDateTime.now());
        userMapper.insert(visitor);

        RoleEnum role = staff == null || staff.getPersonType() == null
                ? RoleEnum.VISITOR : staff.getPersonType().getRole();
        List<UserRole> roles = List.of(createRole(visitor.getId(), role));
        UserRole current = pickDefault(roles);
        String token = jwtUtil.generate(visitor.getId(), visitor.getName(),
                roles.stream().map(UserRole::getRole).toList(), current.getRole(), current.getSchoolId());
        visitor.setLastLoginTime(LocalDateTime.now());
        visitor.setLastLoginIp(ip);
        userMapper.updateById(visitor);
        log.info("H5 自动创建访客账号: phone={}", phone);
        return buildLoginVO(visitor, roles, current, token);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public LoginVO ssoLogin(String code, String redirectUri) {
        IdentityProvider provider = integrationFactory.identity();
        if (!provider.isConfigured()) {
            throw new BizException(400, "统一身份认证未配置");
        }
        IdentityProvider.SsoUser ssoUser = provider.exchange(code, redirectUri);
        if (ssoUser == null || ssoUser.phone() == null || ssoUser.phone().isBlank()) {
            throw new BizException(401, "统一认证未返回手机号");
        }

        User user = findByPhone(ssoUser.phone());
        if (user == null) {
            user = createUser(ssoUser);
        } else if (!Integer.valueOf(1).equals(user.getStatus())) {
            throw new BizException(403, "账号已停用");
        }

        List<UserRole> roles = activeRoles(user.getId());
        if (roles.isEmpty()) {
            roles = List.of(createRole(user.getId(), RoleEnum.of(ssoUser.personType())));
        }
        UserRole current = pickDefault(roles);
        String token = jwtUtil.generate(user.getId(), user.getName(),
                roles.stream().map(UserRole::getRole).toList(), current.getRole(), current.getSchoolId());
        log.info("统一认证登录成功: userId={}, role={}", user.getId(), current.getRole());
        return buildLoginVO(user, roles, current, token);
    }

    @Override
    public UserInfoVO me(Long userId) {
        User user = userMapper.selectById(userId);
        if (user == null) {
            throw new BizException(401, "用户不存在");
        }
        List<UserRole> roles = activeRoles(userId);
        UserRole current = pickDefault(roles);

        UserInfoVO vo = new UserInfoVO();
        vo.setId(user.getId());
        vo.setPhone(user.getPhone());
        vo.setName(user.getName());
        vo.setAvatarUrl(user.getAvatarUrl());
        vo.setRoles(roles.stream().map(role -> RoleVO.of(role.getRole(), role.getSchoolId(),
                Integer.valueOf(1).equals(role.getIsDefault()))).toList());
        RoleEnum contextRole = UserContext.getRole();
        vo.setCurrentRole(contextRole != null ? contextRole.getCode() : current.getRole().getCode());
        vo.setSchoolId(UserContext.getSchoolId() != null ? UserContext.getSchoolId() : current.getSchoolId());
        Staff staff = findStaff(user);
        if (staff != null && staff.getPersonType() != null) {
            PersonTypeEnum personType = staff.getPersonType();
            vo.setPersonType(personType.getCode());
            vo.setPersonTypeDesc(personType.getDescription());
        }
        return vo;
    }

    private Staff findStaff(User user) {
        if (user.getPersonNo() != null && !user.getPersonNo().isBlank()) {
            Staff byNo = staffMapper.selectOne(new LambdaQueryWrapper<Staff>()
                    .eq(Staff::getPersonNo, user.getPersonNo())
                    .eq(Staff::getIsDeleted, 0)
                    .last("LIMIT 1"));
            if (byNo != null) {
                return byNo;
            }
        }
        return staffMapper.selectOne(new LambdaQueryWrapper<Staff>()
                .eq(Staff::getPhone, user.getPhone())
                .eq(Staff::getIsDeleted, 0)
                .last("LIMIT 1"));
    }

    private Staff findActiveStaffByPhone(String phone) {
        return staffMapper.selectOne(new LambdaQueryWrapper<Staff>()
                .eq(Staff::getPhone, phone)
                .eq(Staff::getStatus, 1)
                .eq(Staff::getIsDeleted, 0)
                .last("LIMIT 1"));
    }

    private boolean personNoUsedByOther(String personNo, Long userId) {
        if (personNo == null || personNo.isBlank()) {
            return false;
        }
        Long count = userMapper.selectCount(new LambdaQueryWrapper<User>()
                .eq(User::getPersonNo, personNo)
                .ne(userId != null, User::getId, userId));
        return count != null && count > 0;
    }

    /**
     * 旧数据修复：账号最初按访客创建，后来人员管理中出现了同手机号的学生/教职工。
     * 登录时把唯一的 VISITOR 角色升级为人员类型对应角色，避免“学生身份却只有访客菜单”。
     */
    private void syncStaffIdentity(User user) {
        List<UserRole> roles = activeRoles(user.getId());
        boolean hasNonVisitor = roles.stream().anyMatch(role -> role.getRole() != RoleEnum.VISITOR);
        if (hasNonVisitor) {
            return;
        }
        Staff staff = findActiveStaffByPhone(user.getPhone());
        if (staff == null || staff.getPersonType() == null
                || staff.getPersonType().getRole() == RoleEnum.VISITOR) {
            return;
        }
        RoleEnum target = staff.getPersonType().getRole();
        UserRole visitorRole = roles.stream()
                .filter(role -> role.getRole() == RoleEnum.VISITOR)
                .findFirst().orElse(null);
        if (visitorRole != null) {
            visitorRole.setRole(target);
            visitorRole.setSource("IDENTITY");
            visitorRole.setIsDefault(1);
            visitorRole.setStatus(1);
            visitorRole.setUpdatedAt(LocalDateTime.now());
            userRoleMapper.updateById(visitorRole);
        } else {
            createRole(user.getId(), target);
        }
        if (user.getPersonNo() == null || user.getPersonNo().isBlank()) {
            if (!personNoUsedByOther(staff.getPersonNo(), user.getId())) {
                user.setPersonNo(staff.getPersonNo());
            } else {
                log.warn("人员工号 {} 已被其他账号占用，跳过账号绑定: userId={}", staff.getPersonNo(), user.getId());
            }
        }
        if (user.getName() == null || user.getName().startsWith("访客")) {
            user.setName(staff.getName());
        }
        user.setUpdatedAt(LocalDateTime.now());
        userMapper.updateById(user);
        log.info("登录时同步人员身份: userId={}, role={}", user.getId(), target);
    }

    @Override
    public void logout(String token) {
        try {
            Claims claims = jwtUtil.parse(token);
            long ttlSeconds = (claims.getExpiration().getTime() - System.currentTimeMillis()) / 1000;
            tokenBlacklist.add(claims.getId(), ttlSeconds);
        } catch (Exception e) {
            log.debug("登出时令牌解析失败，忽略: {}", e.getMessage());
        }
    }

    private User findByPhone(String phone) {
        return userMapper.selectOne(new LambdaQueryWrapper<User>().eq(User::getPhone, phone));
    }

    private List<UserRole> activeRoles(Long userId) {
        return userRoleMapper.selectList(new LambdaQueryWrapper<UserRole>()
                .eq(UserRole::getUserId, userId)
                .eq(UserRole::getStatus, 1)
                .orderByAsc(UserRole::getId));
    }

    private UserRole pickDefault(List<UserRole> roles) {
        return roles.stream()
                .filter(role -> Integer.valueOf(1).equals(role.getIsDefault()))
                .findFirst()
                .orElse(roles.get(0));
    }

    private User createUser(IdentityProvider.SsoUser ssoUser) {
        User user = new User();
        user.setPhone(ssoUser.phone());
        user.setName(ssoUser.name() == null ? ssoUser.loginName() : ssoUser.name());
        user.setPassword(passwordEncoder.encode(UUID.randomUUID().toString()));
        user.setStatus(1);
        user.setCreatedAt(LocalDateTime.now());
        user.setUpdatedAt(LocalDateTime.now());
        userMapper.insert(user);

        RoleEnum role = RoleEnum.of(ssoUser.personType());
        createRole(user.getId(), role == null ? RoleEnum.TEACHER : role);
        return user;
    }

    private UserRole createRole(Long userId, RoleEnum role) {
        UserRole userRole = new UserRole();
        userRole.setUserId(userId);
        userRole.setRole(role == null ? RoleEnum.TEACHER : role);
        userRole.setIsDefault(1);
        userRole.setStatus(1);
        userRole.setCreatedAt(LocalDateTime.now());
        userRole.setUpdatedAt(LocalDateTime.now());
        userRoleMapper.insert(userRole);
        return userRole;
    }

    private LoginVO buildLoginVO(User user, List<UserRole> roles, UserRole current, String token) {
        LoginVO vo = new LoginVO();
        vo.setToken(token);
        vo.setExpiresIn(jwtUtil.getExpirationMs() / 1000);
        vo.setUserId(user.getId());
        vo.setName(user.getName());
        vo.setPhone(user.getPhone());
        vo.setCurrentRole(current.getRole().getCode());
        vo.setSchoolId(current.getSchoolId());
        vo.setRoles(roles.stream().map(role -> RoleVO.of(role.getRole(), role.getSchoolId(),
                Integer.valueOf(1).equals(role.getIsDefault()))).toList());
        return vo;
    }
}
