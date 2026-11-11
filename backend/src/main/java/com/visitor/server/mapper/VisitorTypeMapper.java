package com.visitor.server.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.visitor.domain.entity.VisitorType;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface VisitorTypeMapper extends BaseMapper<VisitorType> {
}
