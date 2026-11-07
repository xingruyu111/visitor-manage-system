package com.visitor.server.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.visitor.domain.entity.FormField;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface FormFieldMapper extends BaseMapper<FormField> {
}
