package com.visitor.integration.config;

import com.visitor.domain.dto.IntegrationConfigSaveDTO;
import com.visitor.domain.entity.IntegrationConfig;
import com.visitor.domain.vo.IntegrationConfigVO;
import com.visitor.integration.IntegrationKey;

import java.util.List;
import java.util.Map;

public interface IntegrationConfigService {

    List<IntegrationConfigVO> list();

    IntegrationConfigVO get(String key);

    /** 已启用且状态正常的配置，未配置/关闭时返回 null。 */
    IntegrationConfig getEnabled(IntegrationKey key);

    IntegrationConfigVO save(String key, IntegrationConfigSaveDTO dto);

    /** 连通性测试：未启用或未配置地址时返回 configured=false。 */
    Map<String, Object> test(String key);
}
