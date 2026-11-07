package com.visitor.server.service;

import com.visitor.domain.entity.ParentSlot;

import java.time.LocalDateTime;
import java.util.List;

public interface ParentSlotService {

    List<ParentSlot> list(String visitorPhone);

    ParentSlot create(ParentSlot body);

    ParentSlot update(Long id, ParentSlot body);

    void delete(Long id);

    /** 家长预约必须落在已配置的有效时段内。 */
    void validate(String visitorPhone, LocalDateTime expected, LocalDateTime leave);
}
