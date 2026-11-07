package com.visitor.server.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.visitor.domain.entity.IntegrationLog;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface IntegrationLogMapper extends BaseMapper<IntegrationLog> {
}
