package com.visitor.server.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.visitor.domain.entity.SchoolClass;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface SchoolClassMapper extends BaseMapper<SchoolClass> {
}
