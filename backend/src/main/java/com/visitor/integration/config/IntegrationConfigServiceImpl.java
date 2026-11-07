package com.visitor.integration.config;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.visitor.common.exception.BizException;
import com.visitor.common.util.SecretCipher;
import com.visitor.domain.dto.IntegrationConfigSaveDTO;
import com.visitor.domain.entity.IntegrationConfig;
import com.visitor.domain.vo.IntegrationConfigVO;
import com.visitor.integration.IntegrationKey;
import com.visitor.integration.core.IntegrationCallResult;
import com.visitor.integration.core.IntegrationClient;
import com.visitor.server.mapper.IntegrationConfigMapper;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Service
public class IntegrationConfigServiceImpl implements IntegrationConfigService {

    private static final String MASK = "******";

    private final IntegrationConfigMapper mapper;
    private final SecretCipher secretCipher;
    private final ObjectMapper objectMapper;
    private final IntegrationClient integrationClient;

    public IntegrationConfigServiceImpl(IntegrationConfigMapper mapper, SecretCipher secretCipher,
                                        ObjectMapper objectMapper, IntegrationClient integrationClient) {
        this.mapper = mapper;
        this.secretCipher = secretCipher;
        this.objectMapper = objectMapper;
        this.integrationClient = integrationClient;
    }

    @Override
    public List<IntegrationConfigVO> list() {
        return mapper.selectList(new LambdaQueryWrapper<IntegrationConfig>()
                        .orderByAsc(IntegrationConfig::getId))
                .stream()
                .map(this::toVO)
                .toList();
    }

    @Override
    public IntegrationConfigVO get(String key) {
        return toVO(require(key));
    }

    @Override
    public IntegrationConfig getEnabled(IntegrationKey key) {
        IntegrationConfig config = findByKey(key.name());
        if (config == null || !Integer.valueOf(1).equals(config.getEnabled())
                || !Integer.valueOf(1).equals(config.getStatus())) {
            return null;
        }
        return config;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public IntegrationConfigVO save(String key, IntegrationConfigSaveDTO dto) {
        IntegrationConfig config = require(key);

        if (dto.getProtocol() != null) {
            config.setProtocol(trimToNull(dto.getProtocol()));
        }
        if (dto.getEnabled() != null) {
            config.setEnabled(dto.getEnabled() ? 1 : 0);
        }
        if (dto.getBaseUrl() != null) {
            config.setBaseUrl(trimToNull(dto.getBaseUrl()));
        }
        if (dto.getAuthType() != null) {
            config.setAuthType(trimToNull(dto.getAuthType()));
        }
        if (dto.getAppId() != null) {
            config.setAppId(trimToNull(dto.getAppId()));
        }
        if (dto.getEndpoints() != null) {
            validateJson("endpoints", dto.getEndpoints());
            config.setEndpoints(trimToNull(dto.getEndpoints()));
        }
        if (dto.getFieldMapping() != null) {
            validateJson("fieldMapping", dto.getFieldMapping());
            config.setFieldMapping(trimToNull(dto.getFieldMapping()));
        }
        if (dto.getExtra() != null) {
            validateJson("extra", dto.getExtra());
            config.setExtra(trimToNull(dto.getExtra()));
        }
        if (dto.getTimeoutMs() != null) {
            config.setTimeoutMs(dto.getTimeoutMs());
        }
        if (dto.getRetryTimes() != null) {
            config.setRetryTimes(dto.getRetryTimes());
        }
        if (dto.getStatus() != null) {
            config.setStatus(dto.getStatus());
        }
        if (dto.getRemark() != null) {
            config.setRemark(trimToNull(dto.getRemark()));
        }
        // 空串或掩码表示不修改已有密钥
        if (dto.getAppSecret() != null && !dto.getAppSecret().isBlank() && !MASK.equals(dto.getAppSecret())) {
            config.setAppSecret(secretCipher.encrypt(dto.getAppSecret().trim()));
        }
        config.setUpdatedAt(LocalDateTime.now());
        mapper.updateById(config);
        return toVO(config);
    }

    @Override
    public Map<String, Object> test(String key) {
        IntegrationConfig config = require(key);
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("integrationKey", key);

        if (!Integer.valueOf(1).equals(config.getEnabled())) {
            result.put("configured", false);
            result.put("reachable", false);
            result.put("message", "未启用");
            return result;
        }
        if (config.getBaseUrl() == null || config.getBaseUrl().isBlank()) {
            result.put("configured", false);
            result.put("reachable", false);
            result.put("message", "未配置地址");
            return result;
        }

        int timeout = Math.min(config.getTimeoutMs() == null ? 3000 : config.getTimeoutMs(), 5000);
        IntegrationCallResult call = integrationClient.get(key, config.getBaseUrl(), timeout, 0);
        result.put("configured", true);
        result.put("reachable", call.success());
        result.put("httpStatus", call.httpStatus());
        result.put("costMs", call.costMs());
        if (!call.success()) {
            result.put("error", call.error());
        }
        return result;
    }

    private IntegrationConfig require(String key) {
        IntegrationKey integrationKey = IntegrationKey.of(key);
        if (integrationKey == null) {
            throw new BizException(400, "未知的集成配置项: " + key);
        }
        IntegrationConfig config = findByKey(integrationKey.name());
        if (config == null) {
            throw new BizException(404, "集成配置不存在: " + key);
        }
        return config;
    }

    private IntegrationConfig findByKey(String key) {
        return mapper.selectOne(new LambdaQueryWrapper<IntegrationConfig>()
                .eq(IntegrationConfig::getIntegrationKey, key));
    }

    private IntegrationConfigVO toVO(IntegrationConfig config) {
        IntegrationConfigVO vo = new IntegrationConfigVO();
        BeanUtils.copyProperties(config, vo);
        IntegrationKey key = IntegrationKey.of(config.getIntegrationKey());
        vo.setDescription(key == null ? null : key.getDescription());
        boolean hasSecret = config.getAppSecret() != null && !config.getAppSecret().isEmpty();
        vo.setSecretConfigured(hasSecret);
        vo.setAppSecret(hasSecret ? MASK : null);
        return vo;
    }

    private void validateJson(String field, String value) {
        if (value == null || value.isBlank()) {
            return;
        }
        try {
            objectMapper.readTree(value);
        } catch (Exception e) {
            throw new BizException(400, field + " 不是合法 JSON");
        }
    }

    private String trimToNull(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }
}
