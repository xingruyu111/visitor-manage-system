package com.visitor.server.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.visitor.domain.entity.VisitorPass;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface VisitorPassMapper extends BaseMapper<VisitorPass> {
}
