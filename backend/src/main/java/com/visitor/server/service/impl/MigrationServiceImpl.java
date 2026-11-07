package com.visitor.server.service.impl;

import com.visitor.common.exception.BizException;
import com.visitor.domain.dto.AccessListImportRow;
import com.visitor.domain.dto.VisitorRecordImportRow;
import com.visitor.domain.entity.VisitorRecord;
import com.visitor.server.mapper.VisitorRecordMapper;
import com.visitor.server.service.AccessCheckService;
import com.visitor.server.service.MigrationService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Service
public class MigrationServiceImpl implements MigrationService {

    private static final DateTimeFormatter DATE_TIME = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
    private static final Map<String, Integer> STATUS = Map.of(
            "待审批", 0, "已通过", 1, "已拒绝", 2, "已入校", 3,
            "已离校", 4, "已取消", 5, "已过期", 6);

    private final VisitorRecordMapper recordMapper;
    private final AccessCheckService accessCheckService;

    public MigrationServiceImpl(VisitorRecordMapper recordMapper, AccessCheckService accessCheckService) {
        this.recordMapper = recordMapper;
        this.accessCheckService = accessCheckService;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Map<String, Object> importVisitorRecords(List<VisitorRecordImportRow> rows) {
        int success = 0;
        List<Map<String, Object>> errors = new ArrayList<>();
        if (rows != null) {
            for (int i = 0; i < rows.size(); i++) {
                try {
                    VisitorRecord record = toRecord(rows.get(i));
                    recordMapper.insert(record);
                    success++;
                } catch (Exception e) {
                    errors.add(Map.of("row", i + 2,
                            "reason", e.getMessage() == null ? "导入失败" : e.getMessage()));
                }
            }
        }
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("success", success);
        result.put("failed", errors.size());
        result.put("errors", errors);
        return result;
    }

    @Override
    public Map<String, Object> importAccessList(List<AccessListImportRow> rows) {
        return accessCheckService.importRows(1L, rows);
    }

    private VisitorRecord toRecord(VisitorRecordImportRow row) {
        if (row.getVisitorName() == null || row.getVisitorName().isBlank()) {
            throw new BizException(400, "访客姓名不能为空");
        }
        LocalDateTime expected = parseTime(row.getExpectedTime(), "到访时间");
        LocalDateTime leave = parseTime(row.getLeaveTime(), "离校时间");
        Integer status = STATUS.get(row.getStatus() == null ? "" : row.getStatus().trim());

        VisitorRecord record = new VisitorRecord();
        record.setSchoolId(1L);
        record.setVisitorUserId(0L);
        record.setVisitorName(row.getVisitorName().trim());
        record.setVisitorPhone(row.getVisitorPhone());
        record.setIdCard(row.getIdCard());
        record.setVisitorType("NORMAL");
        record.setTargetName(row.getTargetName());
        record.setVisitPurpose(row.getVisitPurpose());
        record.setPlateNumber(row.getPlateNumber() == null ? null : row.getPlateNumber().trim().toUpperCase());
        record.setExpectedTime(expected);
        record.setLeaveTime(leave);
        record.setStatus(status == null ? 1 : status);
        record.setCreatedAt(LocalDateTime.now());
        record.setUpdatedAt(LocalDateTime.now());
        return record;
    }

    private LocalDateTime parseTime(String value, String label) {
        if (value == null || value.isBlank()) {
            throw new BizException(400, label + "不能为空");
        }
        try {
            String trimmed = value.trim();
            if (trimmed.length() == 10) {
                return LocalDate.parse(trimmed).atTime(9, 0);
            }
            return LocalDateTime.parse(trimmed, DATE_TIME);
        } catch (Exception e) {
            throw new BizException(400, label + "格式应为 yyyy-MM-dd HH:mm:ss");
        }
    }
}
