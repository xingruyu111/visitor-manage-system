package com.visitor.server.service;

import com.visitor.common.core.PageResult;
import com.visitor.domain.entity.AlarmRecord;

public interface AlarmService {

    int checkOverstay();

    PageResult<AlarmRecord> page(Integer handled, int page, int size);

    void handle(Long id, Long operatorId);
}
