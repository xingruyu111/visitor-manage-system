package com.visitor.server.service;

import com.visitor.common.core.PageResult;
import com.visitor.domain.vo.VisitLogVO;

import java.time.LocalDateTime;
import java.util.List;

public interface VisitLogService {

    PageResult<VisitLogVO> page(String direction, String source, LocalDateTime from, LocalDateTime to,
                                int page, int size);

    List<VisitLogVO> exportRows(String direction, String source, LocalDateTime from, LocalDateTime to);
}
