package com.visitor.server.service;

import com.visitor.common.core.PageResult;
import com.visitor.domain.entity.IntegrationLog;

import java.util.List;

public interface InterfaceLogService {

    PageResult<IntegrationLog> page(String integrationKey, Integer success, int page, int size);

    List<IntegrationLog> exportRows(String integrationKey, Integer success);
}
