package com.visitor.security;

import com.visitor.common.exception.BizException;

/**
 * 数据范围：单校部署，所有业务数据固定属于 school_id = 1。
 * 保留 schoolId 入参仅为兼容旧接口与历史数据。
 */
public final class SchoolScopes {

    /** 单校部署的固定学校 ID。 */
    public static final Long DEFAULT_SCHOOL_ID = 1L;

    private SchoolScopes() {
    }

    public static Long resolve(Long requestedSchoolId) {
        if (requestedSchoolId != null && !DEFAULT_SCHOOL_ID.equals(requestedSchoolId)) {
            throw new BizException(400, "当前为单校部署，仅支持 schoolId=1");
        }
        return DEFAULT_SCHOOL_ID;
    }
}
