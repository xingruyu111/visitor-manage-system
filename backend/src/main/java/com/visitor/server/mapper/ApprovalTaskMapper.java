package com.visitor.server.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.visitor.domain.entity.ApprovalTask;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface ApprovalTaskMapper extends BaseMapper<ApprovalTask> {
}
