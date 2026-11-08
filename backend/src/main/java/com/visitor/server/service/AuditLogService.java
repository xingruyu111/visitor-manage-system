package com.visitor.server.service;

import com.visitor.common.core.PageResult;
import com.visitor.domain.entity.AuditLog;

import java.util.List;

public interface AuditLogService {

    void record(String action, String bizType, Long bizId, String detail);

    PageResult<AuditLog> page(String action, String bizType, String keyword, int page, int size);

    List<AuditLog> exportRows(String action, String bizType, String keyword);
}
