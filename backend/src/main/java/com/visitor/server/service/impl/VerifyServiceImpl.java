package com.visitor.server.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.UpdateWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.visitor.common.core.PageResult;
import com.visitor.common.exception.BizException;
import com.visitor.domain.entity.VisitLog;
import com.visitor.domain.entity.VisitorPass;
import com.visitor.domain.entity.VisitorRecord;
import com.visitor.server.mapper.VisitLogMapper;
import com.visitor.server.mapper.VisitorPassMapper;
import com.visitor.server.mapper.VisitorRecordMapper;
import com.visitor.server.service.VerifyService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.Map;

@Service
public class VerifyServiceImpl implements VerifyService {

    private final VisitorPassMapper passMapper;
    private final VisitorRecordMapper recordMapper;
    private final VisitLogMapper visitLogMapper;

    public VerifyServiceImpl(VisitorPassMapper passMapper, VisitorRecordMapper recordMapper,
                             VisitLogMapper visitLogMapper) {
        this.passMapper = passMapper;
        this.recordMapper = recordMapper;
        this.visitLogMapper = visitLogMapper;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Map<String, Object> verify(Long guardId, String qrToken) {
        VisitorPass pass = passMapper.selectOne(new LambdaQueryWrapper<VisitorPass>()
                .eq(VisitorPass::getQrToken, qrToken));
        if (pass == null) {
            throw new BizException(404, "无效的入校码");
        }
        if (!Integer.valueOf(0).equals(pass.getStatus())) {
            throw new BizException(400, "凭证已失效");
        }
        LocalDateTime now = LocalDateTime.now();
        if (now.isBefore(pass.getValidFrom())) {
            throw new BizException(400, "未到入校时间");
        }
        if (now.isAfter(pass.getValidTo())) {
            throw new BizException(400, "凭证已过期");
        }

        VisitorRecord record = recordMapper.selectById(pass.getRecordId());
        if (record == null) {
            throw new BizException(404, "预约记录不存在");
        }
        String direction = switch (record.getStatus()) {
            case 1 -> "IN";
            case 3 -> "OUT";
            case 4 -> "IN";
            default -> throw new BizException(400, "当前状态不可核销");
        };

        if ("IN".equals(direction)) {
            int updated = passMapper.update(null, new UpdateWrapper<VisitorPass>()
                    .eq("id", pass.getId())
                    .lt("used_entries", pass.getMaxEntries())
                    .setSql("used_entries = used_entries + 1"));
            if (updated == 0) {
                throw new BizException(400, "通行次数已用完");
            }
            record.setStatus(3);
        } else {
            record.setStatus(4);
        }
        record.setUpdatedAt(now);
        recordMapper.updateById(record);

        VisitLog log = new VisitLog();
        log.setSchoolId(record.getSchoolId());
        log.setRecordId(record.getId());
        log.setPassId(pass.getId());
        log.setDirection(direction);
        log.setSource("LOCAL");
        log.setGuardId(guardId);
        log.setEventTime(now);
        log.setCreatedAt(now);
        visitLogMapper.insert(log);

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("direction", direction);
        result.put("recordId", record.getId());
        result.put("recordStatus", record.getStatus());
        result.put("visitorName", record.getVisitorName());
        return result;
    }

    @Override
    public PageResult<VisitLog> records(Long schoolId, int page, int size) {
        int safePage = Math.max(page, 1);
        int safeSize = Math.min(Math.max(size, 1), 100);
        Page<VisitLog> result = visitLogMapper.selectPage(new Page<>(safePage, safeSize),
                new LambdaQueryWrapper<VisitLog>()
                        .eq(VisitLog::getSchoolId, schoolId)
                        .orderByDesc(VisitLog::getId));
        return PageResult.of(result.getTotal(), safePage, safeSize, result.getRecords());
    }
}
