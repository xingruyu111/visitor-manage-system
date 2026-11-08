package com.visitor.server.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.visitor.common.core.PageResult;
import com.visitor.domain.entity.AuditLog;
import com.visitor.security.UserContext;
import com.visitor.server.mapper.AuditLogMapper;
import com.visitor.server.service.AuditLogService;
import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import java.time.LocalDateTime;
import java.util.List;

@Slf4j
@Service
public class AuditLogServiceImpl implements AuditLogService {

    private final AuditLogMapper mapper;

    public AuditLogServiceImpl(AuditLogMapper mapper) {
        this.mapper = mapper;
    }

    @Override
    public void record(String action, String bizType, Long bizId, String detail) {
        try {
            AuditLog entity = new AuditLog();
            entity.setUserId(UserContext.getUserId());
            entity.setUserName(UserContext.getName());
            entity.setAction(action);
            entity.setBizType(bizType);
            entity.setBizId(bizId);
            entity.setDetail(truncate(detail));
            entity.setIp(currentIp());
            entity.setCreatedAt(LocalDateTime.now());
            mapper.insert(entity);
        } catch (Exception e) {
            // 审计失败不影响主流程
            log.warn("审计日志写入失败: action={}, err={}", action, e.getMessage());
        }
    }

    @Override
    public PageResult<AuditLog> page(String action, String bizType, String keyword, int page, int size) {
        int safePage = Math.max(page, 1);
        int safeSize = Math.min(Math.max(size, 1), 100);
        Page<AuditLog> result = mapper.selectPage(new Page<>(safePage, safeSize), wrapper(action, bizType, keyword));
        return PageResult.of(result.getTotal(), safePage, safeSize, result.getRecords());
    }

    @Override
    public List<AuditLog> exportRows(String action, String bizType, String keyword) {
        return mapper.selectList(wrapper(action, bizType, keyword)
                .orderByDesc(AuditLog::getId)
                .last("LIMIT 5000"));
    }

    private LambdaQueryWrapper<AuditLog> wrapper(String action, String bizType, String keyword) {
        return new LambdaQueryWrapper<AuditLog>()
                .eq(action != null && !action.isBlank(), AuditLog::getAction, action)
                .eq(bizType != null && !bizType.isBlank(), AuditLog::getBizType, bizType)
                .and(keyword != null && !keyword.isBlank(), w -> w
                        .like(AuditLog::getUserName, keyword)
                        .or().like(AuditLog::getDetail, keyword))
                .orderByDesc(AuditLog::getId);
    }

    private String currentIp() {
        try {
            ServletRequestAttributes attributes =
                    (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
            if (attributes == null) {
                return null;
            }
            HttpServletRequest request = attributes.getRequest();
            String forwarded = request.getHeader("X-Forwarded-For");
            if (forwarded != null && !forwarded.isBlank()) {
                return forwarded.split(",")[0].trim();
            }
            return request.getRemoteAddr();
        } catch (Exception e) {
            return null;
        }
    }

    private String truncate(String detail) {
        if (detail == null) {
            return null;
        }
        return detail.length() > 500 ? detail.substring(0, 500) : detail;
    }
}
