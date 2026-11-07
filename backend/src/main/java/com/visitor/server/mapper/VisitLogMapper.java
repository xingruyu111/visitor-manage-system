package com.visitor.server.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.visitor.domain.entity.VisitLog;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface VisitLogMapper extends BaseMapper<VisitLog> {
}
