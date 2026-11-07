package com.visitor.server.service;

import com.visitor.common.core.PageResult;
import com.visitor.domain.entity.AccessList;

public interface AccessCheckService {

    /** 返回 BLACKLIST / WHITELIST / NONE */
    String check(Long schoolId, String phone, String idCard, String plate);

    PageResult<AccessList> page(Long schoolId, Integer listType, String targetType, String keyword,
                                int page, int size);

    AccessList create(Long schoolId, AccessList body);

    AccessList update(Long schoolId, Long id, AccessList body);

    void delete(Long schoolId, Long id);

    int expireOverdue();
}
