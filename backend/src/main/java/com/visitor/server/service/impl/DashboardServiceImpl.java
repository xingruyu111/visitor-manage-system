package com.visitor.server.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.visitor.domain.entity.AccessList;
import com.visitor.domain.entity.ApprovalTask;
import com.visitor.domain.entity.VisitorRecord;
import com.visitor.server.mapper.AccessListMapper;
import com.visitor.server.mapper.ApprovalTaskMapper;
import com.visitor.server.mapper.VisitorRecordMapper;
import com.visitor.server.service.DashboardService;
import com.visitor.workflow.ApprovalStatuses;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.Map;

@Service
public class DashboardServiceImpl implements DashboardService {

    private final VisitorRecordMapper recordMapper;
    private final ApprovalTaskMapper taskMapper;
    private final AccessListMapper accessListMapper;

    public DashboardServiceImpl(VisitorRecordMapper recordMapper, ApprovalTaskMapper taskMapper,
                                AccessListMapper accessListMapper) {
        this.recordMapper = recordMapper;
        this.taskMapper = taskMapper;
        this.accessListMapper = accessListMapper;
    }

    @Override
    public Map<String, Object> summary() {
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("total", countRecords(null));
        result.put("pending", countRecords(0));
        result.put("approved", countRecords(1));
        result.put("rejected", countRecords(2));
        result.put("inSchool", countRecords(3));
        result.put("left", countRecords(4));
        result.put("expired", countRecords(6));
        result.put("today", recordMapper.selectCount(new LambdaQueryWrapper<VisitorRecord>()
                .ge(VisitorRecord::getExpectedTime, LocalDate.now().atStartOfDay())
                .lt(VisitorRecord::getExpectedTime, LocalDate.now().plusDays(1).atStartOfDay())));
        result.put("pendingTasks", taskMapper.selectCount(new LambdaQueryWrapper<ApprovalTask>()
                .eq(ApprovalTask::getStatus, ApprovalStatuses.PENDING)));
        result.put("activeBlacklist", accessListMapper.selectCount(new LambdaQueryWrapper<AccessList>()
                .eq(AccessList::getListType, 1)
                .eq(AccessList::getStatus, 1)));
        result.put("generatedAt", LocalDateTime.now());
        return result;
    }

    private Long countRecords(Integer status) {
        return recordMapper.selectCount(new LambdaQueryWrapper<VisitorRecord>()
                .eq(status != null, VisitorRecord::getStatus, status));
    }
}
