package com.visitor.server.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.visitor.domain.entity.User;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface UserMapper extends BaseMapper<User> {
}
