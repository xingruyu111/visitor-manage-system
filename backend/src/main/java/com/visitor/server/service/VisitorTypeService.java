package com.visitor.server.service;

import com.visitor.domain.dto.BookingRule;
import com.visitor.domain.dto.VisitorTypeSaveDTO;
import com.visitor.domain.entity.VisitorType;

import java.time.LocalDateTime;
import java.util.List;

public interface VisitorTypeService {

    List<VisitorType> list(boolean onlyEnabled);

    VisitorType create(VisitorTypeSaveDTO dto);

    VisitorType update(Long id, VisitorTypeSaveDTO dto);

    VisitorType updateStatus(Long id, Integer status);

    List<BookingRule> rulesOf(String code);

    void validateBookingTime(String code, LocalDateTime expected, LocalDateTime leave);

    int passMaxEntries(String code);
}
