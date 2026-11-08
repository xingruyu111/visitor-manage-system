package com.visitor.server.service;

import com.visitor.common.core.PageResult;
import com.visitor.domain.entity.IpBlacklist;

import java.time.LocalDateTime;

public interface IpBlacklistService {

    PageResult<IpBlacklist> page(String keyword, int page, int size);

    IpBlacklist add(String ip, String reason, LocalDateTime expireAt, Long operatorId);

    void remove(Long id);

    /** 命中且未过期抛 429。 */
    void assertAllowed(String ip);
}
