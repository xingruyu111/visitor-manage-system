package com.visitor.integration.core;

import com.visitor.common.core.PageResult;
import com.visitor.domain.entity.IntegrationLog;

public interface IntegrationLogService {

    /** 记录一次调用；失败不抛出，避免影响主流程。 */
    void record(IntegrationLog log);

    PageResult<IntegrationLog> page(String integrationKey, int page, int size);
}
