package com.visitor.server.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.visitor.domain.entity.AccessList;
import com.visitor.domain.entity.ApprovalTask;
import com.visitor.domain.entity.VisitorRecord;
import com.visitor.server.mapper.AccessListMapper;
import com.visitor.server.mapper.AuditLogMapper;
import com.visitor.server.mapper.IntegrationLogMapper;
import com.visitor.domain.entity.AuditLog;
import com.visitor.domain.entity.IntegrationLog;
import com.visitor.server.mapper.ApprovalTaskMapper;
import com.visitor.server.mapper.VisitorRecordMapper;
import com.visitor.server.service.DashboardService;
import com.visitor.workflow.ApprovalStatuses;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Service
public class DashboardServiceImpl implements DashboardService {

    private final VisitorRecordMapper recordMapper;
    private final ApprovalTaskMapper taskMapper;
    private final AccessListMapper accessListMapper;
    private final AuditLogMapper auditLogMapper;
    private final IntegrationLogMapper integrationLogMapper;

    public DashboardServiceImpl(VisitorRecordMapper recordMapper, ApprovalTaskMapper taskMapper,
                                AccessListMapper accessListMapper, AuditLogMapper auditLogMapper, IntegrationLogMapper integrationLogMapper) {
        this.recordMapper = recordMapper;
        this.taskMapper = taskMapper;
        this.accessListMapper = accessListMapper;
        this.auditLogMapper = auditLogMapper;
        this.integrationLogMapper = integrationLogMapper;
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

    @Override
    public Map<String, Object> trend(int days) {
        int span = Math.min(Math.max(days, 1), 90);
        LocalDate end = LocalDate.now();
        List<Map<String, Object>> daily = new ArrayList<>();
        long approved = 0;
        long rejected = 0;
        for (int i = span - 1; i >= 0; i--) {
            LocalDate date = end.minusDays(i);
            LocalDateTime start = date.atStartOfDay();
            LocalDateTime endTime = start.plusDays(1);
            long total = countRange(start, endTime, null);
            long approvedDay = countRange(start, endTime, 1);
            long rejectedDay = countRange(start, endTime, 2);
            long leftDay = countRange(start, endTime, 4);
            approved += approvedDay;
            rejected += rejectedDay;
            Map<String, Object> row = new LinkedHashMap<>();
            row.put("date", date.toString());
            row.put("total", total);
            row.put("approved", approvedDay);
            row.put("rejected", rejectedDay);
            row.put("left", leftDay);
            daily.add(row);
        }
        LocalDateTime rangeStart = end.minusDays(span - 1L).atStartOfDay();
        long blacklistHits = auditLogMapper.selectCount(new LambdaQueryWrapper<AuditLog>()
                .eq(AuditLog::getAction, "BLACKLIST_HIT")
                .ge(AuditLog::getCreatedAt, rangeStart));
        long integrationErrors = integrationLogMapper.selectCount(new LambdaQueryWrapper<IntegrationLog>()
                .eq(IntegrationLog::getSuccess, 0)
                .ge(IntegrationLog::getCreatedAt, rangeStart));
        double rate = approved + rejected == 0 ? 0
                : Math.round(approved * 10000.0 / (approved + rejected)) / 100.0;
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("days", span);
        result.put("daily", daily);
        result.put("approvalRate", rate);
        result.put("blacklistHits", blacklistHits);
        result.put("integrationErrors", integrationErrors);
        return result;
    }

    private long countRange(LocalDateTime start, LocalDateTime end, Integer status) {
        return recordMapper.selectCount(new LambdaQueryWrapper<VisitorRecord>()
                .ge(VisitorRecord::getExpectedTime, start)
                .lt(VisitorRecord::getExpectedTime, end)
                .eq(status != null, VisitorRecord::getStatus, status));
    }
    private Long countRecords(Integer status) {
        return recordMapper.selectCount(new LambdaQueryWrapper<VisitorRecord>()
                .eq(status != null, VisitorRecord::getStatus, status));
    }
}
