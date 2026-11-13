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
import java.time.YearMonth;
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
    public Map<String, Object> screen() {
        Map<String, Object> result = new LinkedHashMap<>();
        Map<String, Object> summary = summary();
        result.put("summary", summary);
        result.put("trend", trend(14));
        result.put("monthly", monthly(6));

        List<Map<String, Object>> statuses = new ArrayList<>();
        statuses.add(statusItem("待审批", summary.get("pending")));
        statuses.add(statusItem("已通过", summary.get("approved")));
        statuses.add(statusItem("已驳回", summary.get("rejected")));
        statuses.add(statusItem("在校中", summary.get("inSchool")));
        statuses.add(statusItem("已离校", summary.get("left")));
        statuses.add(statusItem("已过期", summary.get("expired")));
        result.put("statusDistribution", statuses);

        result.put("visitorTypeDistribution", distribution("visitor_type"));
        result.put("targetTypeDistribution", distribution("target_type"));

        LocalDate today = LocalDate.now();
        List<Map<String, Object>> hourly = new ArrayList<>();
        for (int hour = 0; hour < 24; hour++) {
            long total = recordMapper.selectCount(new LambdaQueryWrapper<VisitorRecord>()
                    .ge(VisitorRecord::getExpectedTime, today.atTime(hour, 0))
                    .lt(VisitorRecord::getExpectedTime, today.atTime(hour, 0).plusHours(1)));
            Map<String, Object> row = new LinkedHashMap<>();
            row.put("hour", hour);
            row.put("total", total);
            hourly.add(row);
        }
        result.put("hourly", hourly);

        result.put("topTargets", recordMapper.selectMaps(
                new com.baomidou.mybatisplus.core.conditions.query.QueryWrapper<VisitorRecord>()
                        .select("target_name AS name", "COUNT(*) AS total")
                        .isNotNull("target_name")
                        .ne("target_name", "")
                        .groupBy("target_name")
                        .orderByDesc("total")
                        .last("LIMIT 5")));
        result.put("recent", recordMapper.selectList(new LambdaQueryWrapper<VisitorRecord>()
                .orderByDesc(VisitorRecord::getId)
                .last("LIMIT 8")));
        return result;
    }

    private Map<String, Object> statusItem(String name, Object value) {
        Map<String, Object> item = new LinkedHashMap<>();
        item.put("name", name);
        item.put("value", value == null ? 0 : value);
        return item;
    }

    private List<Map<String, Object>> distribution(String column) {
        return recordMapper.selectMaps(
                new com.baomidou.mybatisplus.core.conditions.query.QueryWrapper<VisitorRecord>()
                        .select(column + " AS type", "COUNT(*) AS total")
                        .isNotNull(column)
                        .ne(column, "")
                        .groupBy(column)
                        .orderByDesc("total"));
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

    @Override
    public List<Map<String, Object>> monthly(int months) {
        int span = Math.min(Math.max(months, 1), 24);
        YearMonth current = YearMonth.now();
        List<Map<String, Object>> result = new ArrayList<>();
        for (int i = span - 1; i >= 0; i--) {
            YearMonth month = current.minusMonths(i);
            LocalDateTime start = month.atDay(1).atStartOfDay();
            LocalDateTime end = month.plusMonths(1).atDay(1).atStartOfDay();
            long total = countRange(start, end, null);
            long approved = countRange(start, end, 1);
            long rejected = countRange(start, end, 2);
            long left = countRange(start, end, 4);
            double rate = approved + rejected == 0 ? 0
                    : Math.round(approved * 10000.0 / (approved + rejected)) / 100.0;
            Map<String, Object> row = new LinkedHashMap<>();
            row.put("month", month.toString());
            row.put("total", total);
            row.put("approved", approved);
            row.put("rejected", rejected);
            row.put("left", left);
            row.put("approvalRate", rate);
            result.add(row);
        }
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
