package com.visitor.server.service;

import com.visitor.common.core.PageResult;
import com.visitor.domain.entity.AlarmRecord;

public interface AlarmService {

    int checkOverstay();

    PageResult<AlarmRecord> page(Integer handled, int page, int size);

    void handle(Long id, Long operatorId);

    /** 黑名单命中时通知保卫处。 */
    void notifyBlacklistHit(String visitorName, String phone);
}
