package com.visitor.server.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.visitor.domain.entity.MpConfig;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface MpConfigMapper extends BaseMapper<MpConfig> {
}
