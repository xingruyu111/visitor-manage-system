package com.visitor.server.service;

import com.visitor.common.core.PageResult;
import com.visitor.domain.entity.VisitLog;

import java.util.Map;

public interface VerifyService {

    Map<String, Object> verify(Long guardId, String qrToken);

    PageResult<VisitLog> records(Long schoolId, int page, int size);
}
