package com.visitor.server.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.visitor.domain.entity.IntegrationConfig;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface IntegrationConfigMapper extends BaseMapper<IntegrationConfig> {
}
