package com.visitor.server.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.visitor.domain.entity.IpBlacklist;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface IpBlacklistMapper extends BaseMapper<IpBlacklist> {
}
