package com.visitor.server.service;

import com.visitor.common.core.PageResult;
import com.visitor.domain.dto.VisitorApplyDTO;
import com.visitor.domain.dto.VisitorBatchDTO;
import com.visitor.domain.entity.Staff;
import com.visitor.domain.entity.VisitorPass;
import com.visitor.domain.entity.VisitorRecord;

import java.util.Map;

public interface VisitorService {

    Map<String, Object> apply(Long userId, VisitorApplyDTO dto);

    PageResult<VisitorRecord> myRecords(Long userId, int page, int size);

    VisitorRecord detail(Long userId, Long id);

    VisitorPass pass(Long userId, Long recordId);

    void cancel(Long userId, Long id);

    PageResult<VisitorRecord> adminPage(Long schoolId, Integer status, String keyword, int page, int size);

    int batchCreate(Long schoolId, VisitorBatchDTO dto, Long operatorId);

    int deptBatch(Long schoolId, VisitorBatchDTO dto, Long operatorId);

    int expireOverdue();

    java.util.List<VisitorRecord> queryByPlate(String plate);

    java.util.List<Staff> queryTargets(String keyword);

    java.util.List<com.visitor.domain.vo.VisitorRecordExportRow> exportRows(Integer status, String keyword);

    java.util.Map<String, Object> preCheck(Long userId, String plate);
}
