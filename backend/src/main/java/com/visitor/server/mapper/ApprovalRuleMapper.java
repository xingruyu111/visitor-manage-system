package com.visitor.server.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.visitor.domain.entity.ApprovalRule;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface ApprovalRuleMapper extends BaseMapper<ApprovalRule> {
}
