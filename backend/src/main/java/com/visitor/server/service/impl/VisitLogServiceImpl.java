package com.visitor.server.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.visitor.common.core.PageResult;
import com.visitor.domain.entity.VisitLog;
import com.visitor.domain.entity.VisitorRecord;
import com.visitor.domain.vo.VisitLogVO;
import com.visitor.server.mapper.VisitLogMapper;
import com.visitor.server.mapper.VisitorRecordMapper;
import com.visitor.server.service.VisitLogService;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
public class VisitLogServiceImpl implements VisitLogService {

    private final VisitLogMapper visitLogMapper;
    private final VisitorRecordMapper recordMapper;

    public VisitLogServiceImpl(VisitLogMapper visitLogMapper, VisitorRecordMapper recordMapper) {
        this.visitLogMapper = visitLogMapper;
        this.recordMapper = recordMapper;
    }

    @Override
    public PageResult<VisitLogVO> page(String direction, String source, LocalDateTime from, LocalDateTime to,
                                       int page, int size) {
        int safePage = Math.max(page, 1);
        int safeSize = Math.min(Math.max(size, 1), 100);
        Page<VisitLog> result = visitLogMapper.selectPage(new Page<>(safePage, safeSize),
                wrapper(direction, source, from, to));
        return PageResult.of(result.getTotal(), safePage, safeSize, toVos(result.getRecords()));
    }

    @Override
    public List<VisitLogVO> exportRows(String direction, String source, LocalDateTime from, LocalDateTime to) {
        return toVos(visitLogMapper.selectList(wrapper(direction, source, from, to)
                .orderByDesc(VisitLog::getId)
                .last("LIMIT 5000")));
    }

    private LambdaQueryWrapper<VisitLog> wrapper(String direction, String source,
                                                 LocalDateTime from, LocalDateTime to) {
        return new LambdaQueryWrapper<VisitLog>()
                .eq(direction != null && !direction.isBlank(), VisitLog::getDirection, direction)
                .eq(source != null && !source.isBlank(), VisitLog::getSource, source)
                .ge(from != null, VisitLog::getEventTime, from)
                .le(to != null, VisitLog::getEventTime, to)
                .orderByDesc(VisitLog::getId);
    }

    private List<VisitLogVO> toVos(List<VisitLog> logs) {
        List<VisitLogVO> rows = new ArrayList<>();
        if (logs.isEmpty()) {
            return rows;
        }
        List<Long> recordIds = logs.stream().map(VisitLog::getRecordId).distinct().toList();
        Map<Long, VisitorRecord> records = new HashMap<>();
        for (VisitorRecord record : recordMapper.selectBatchIds(recordIds)) {
            records.put(record.getId(), record);
        }
        for (VisitLog log : logs) {
            VisitLogVO vo = new VisitLogVO();
            vo.setId(log.getId());
            vo.setRecordId(log.getRecordId());
            vo.setDirection(log.getDirection());
            vo.setSource(log.getSource());
            vo.setExternalId(log.getExternalId());
            vo.setEventTime(log.getEventTime());
            VisitorRecord record = records.get(log.getRecordId());
            if (record != null) {
                vo.setVisitorName(record.getVisitorName());
                vo.setPlateNumber(record.getPlateNumber());
            }
            rows.add(vo);
        }
        return rows;
    }
}
