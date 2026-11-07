package com.visitor.server.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.visitor.domain.entity.VisitorRecord;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface VisitorRecordMapper extends BaseMapper<VisitorRecord> {
}
