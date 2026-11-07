package com.visitor.server.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.visitor.common.enums.RoleEnum;
import com.visitor.common.exception.BizException;
import com.visitor.domain.dto.WechatLoginDTO;
import com.visitor.domain.entity.MpConfig;
import com.visitor.domain.entity.User;
import com.visitor.domain.entity.UserRole;
import com.visitor.security.JwtUtil;
import com.visitor.server.mapper.UserMapper;
import com.visitor.server.mapper.UserRoleMapper;
import com.visitor.server.service.MpConfigService;
import com.visitor.server.service.WechatLoginService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.RestTemplate;

import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 公众号网页授权登录：MOCK 模式用 code 生成 openid，REST 模式换取真实 openid。
 */
@Slf4j
@Service
public class WechatLoginServiceImpl implements WechatLoginService {

    private final MpConfigService mpConfigService;
    private final UserMapper userMapper;
    private final UserRoleMapper userRoleMapper;
    private final JwtUtil jwtUtil;

    public WechatLoginServiceImpl(MpConfigService mpConfigService, UserMapper userMapper,
                                  UserRoleMapper userRoleMapper, JwtUtil jwtUtil) {
        this.mpConfigService = mpConfigService;
        this.userMapper = userMapper;
        this.userRoleMapper = userRoleMapper;
        this.jwtUtil = jwtUtil;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Map<String, Object> login(WechatLoginDTO dto) {
        MpConfig config = mpConfigService.get();
        if (config == null || config.getAppId() == null || config.getAppId().isBlank()) {
            throw new BizException(400, "微信登录未配置公众号");
        }
        String openid = "REST".equalsIgnoreCase(config.getAuthMode())
                ? exchangeOpenid(config, dto.getCode())
                : "mock-" + dto.getCode();

        User user = userMapper.selectOne(new LambdaQueryWrapper<User>()
                .eq(User::getMpOpenid, openid).last("LIMIT 1"));
        if (user == null) {
            user = userMapper.selectOne(new LambdaQueryWrapper<User>()
                    .eq(User::getPhone, dto.getPhone()).last("LIMIT 1"));
        }
        if (user == null) {
            user = new User();
            user.setPhone(dto.getPhone());
            user.setName("微信访客" + dto.getPhone().substring(Math.max(0, dto.getPhone().length() - 4)));
            user.setMpOpenid(openid);
            user.setStatus(1);
            user.setCreatedAt(LocalDateTime.now());
            user.setUpdatedAt(LocalDateTime.now());
            userMapper.insert(user);
        } else if (user.getMpOpenid() == null || user.getMpOpenid().isBlank()) {
            user.setMpOpenid(openid);
            user.setUpdatedAt(LocalDateTime.now());
            userMapper.updateById(user);
        }

        RoleEnum role = ensureVisitorRole(user.getId());
        String token = jwtUtil.generate(user.getId(), user.getName(), List.of(role), role, 1L);
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("token", token);
        result.put("userId", user.getId());
        result.put("name", user.getName());
        result.put("currentRole", role.getCode());
        result.put("schoolId", 1L);
        result.put("openidBound", true);
        return result;
    }

    private RoleEnum ensureVisitorRole(Long userId) {
        List<UserRole> roles = userRoleMapper.selectList(new LambdaQueryWrapper<UserRole>()
                .eq(UserRole::getUserId, userId).eq(UserRole::getStatus, 1));
        if (!roles.isEmpty()) {
            return roles.get(0).getRole();
        }
        UserRole role = new UserRole();
        role.setUserId(userId);
        role.setRole(RoleEnum.VISITOR);
        role.setSchoolId(1L);
        role.setIsDefault(1);
        role.setStatus(1);
        role.setCreatedAt(LocalDateTime.now());
        role.setUpdatedAt(LocalDateTime.now());
        userRoleMapper.insert(role);
        return RoleEnum.VISITOR;
    }

    private String exchangeOpenid(MpConfig config, String code) {
        String url = "https://api.weixin.qq.com/sns/oauth2/access_token?appid=" + config.getAppId()
                + "&secret=" + config.getSecret() + "&code=" + code + "&grant_type=authorization_code";
        try {
            Map<?, ?> body = new RestTemplate().getForObject(url, Map.class);
            if (body == null || body.get("openid") == null) {
                throw new BizException(400, "微信授权失败：" + (body == null ? "无响应" : body.get("errmsg")));
            }
            return body.get("openid").toString();
        } catch (BizException e) {
            throw e;
        } catch (Exception e) {
            log.warn("微信授权请求失败: {}", e.getMessage());
            throw new BizException(400, "微信授权请求失败，请稍后重试");
        }
    }
}
