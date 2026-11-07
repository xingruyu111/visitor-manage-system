package com.visitor.server.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.visitor.common.exception.BizException;
import com.visitor.domain.entity.ParentSlot;
import com.visitor.server.mapper.ParentSlotMapper;
import com.visitor.server.service.ParentSlotService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;

@Service
public class ParentSlotServiceImpl implements ParentSlotService {

    private final ParentSlotMapper mapper;

    public ParentSlotServiceImpl(ParentSlotMapper mapper) {
        this.mapper = mapper;
    }

    @Override
    public List<ParentSlot> list(String visitorPhone) {
        return mapper.selectList(new LambdaQueryWrapper<ParentSlot>()
                .eq(visitorPhone != null && !visitorPhone.isBlank(), ParentSlot::getVisitorPhone, visitorPhone)
                .orderByAsc(ParentSlot::getWeekday)
                .orderByAsc(ParentSlot::getStartTime));
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public ParentSlot create(ParentSlot body) {
        validateBody(body);
        ParentSlot entity = new ParentSlot();
        copy(body, entity);
        entity.setStatus(1);
        entity.setCreatedAt(LocalDateTime.now());
        entity.setUpdatedAt(LocalDateTime.now());
        mapper.insert(entity);
        return entity;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public ParentSlot update(Long id, ParentSlot body) {
        ParentSlot entity = mapper.selectById(id);
        if (entity == null) {
            throw new BizException(404, "时段不存在");
        }
        validateBody(body);
        copy(body, entity);
        entity.setUpdatedAt(LocalDateTime.now());
        mapper.updateById(entity);
        return entity;
    }

    @Override
    public void delete(Long id) {
        if (mapper.selectById(id) == null) {
            throw new BizException(404, "时段不存在");
        }
        mapper.deleteById(id);
    }

    @Override
    public void validate(String visitorPhone, LocalDateTime expected, LocalDateTime leave) {
        List<ParentSlot> slots = mapper.selectList(new LambdaQueryWrapper<ParentSlot>()
                .eq(ParentSlot::getVisitorPhone, visitorPhone)
                .eq(ParentSlot::getStatus, 1));
        if (slots.isEmpty()) {
            throw new BizException(400, "该家长未配置固定时段，暂不能预约");
        }
        LocalDate date = expected.toLocalDate();
        LocalTime start = expected.toLocalTime();
        LocalTime end = leave.toLocalTime();
        boolean sameDay = leave.toLocalDate().equals(date);
        for (ParentSlot slot : slots) {
            if (slot.getWeekday() != null && slot.getWeekday() != date.getDayOfWeek().getValue()) {
                continue;
            }
            if (slot.getEffectiveFrom() != null && date.isBefore(slot.getEffectiveFrom())) {
                continue;
            }
            if (slot.getEffectiveTo() != null && date.isAfter(slot.getEffectiveTo())) {
                continue;
            }
            if (sameDay && !start.isBefore(slot.getStartTime()) && !end.isAfter(slot.getEndTime())) {
                return;
            }
        }
        throw new BizException(400, "不在家长固定时段内，请按学校安排的时间预约");
    }

    private void validateBody(ParentSlot body) {
        if (body.getVisitorPhone() == null || body.getVisitorPhone().isBlank()) {
            throw new BizException(400, "家长手机号不能为空");
        }
        if (body.getStartTime() == null || body.getEndTime() == null
                || !body.getEndTime().isAfter(body.getStartTime())) {
            throw new BizException(400, "结束时间必须晚于开始时间");
        }
        if (body.getWeekday() != null && (body.getWeekday() < 1 || body.getWeekday() > 7)) {
            throw new BizException(400, "星期取值必须是 1-7");
        }
    }

    private void copy(ParentSlot from, ParentSlot to) {
        to.setVisitorPhone(from.getVisitorPhone().trim());
        to.setWeekday(from.getWeekday());
        to.setStartTime(from.getStartTime());
        to.setEndTime(from.getEndTime());
        to.setEffectiveFrom(from.getEffectiveFrom());
        to.setEffectiveTo(from.getEffectiveTo());
        if (from.getStatus() != null) {
            to.setStatus(from.getStatus());
        }
    }
}
