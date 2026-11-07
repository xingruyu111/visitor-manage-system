package com.visitor.server.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.visitor.domain.entity.AccessList;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface AccessListMapper extends BaseMapper<AccessList> {
}
