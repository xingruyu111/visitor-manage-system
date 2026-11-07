package com.visitor.server.service;

import com.visitor.common.core.PageResult;
import com.visitor.domain.dto.StaffSaveDTO;
import com.visitor.domain.vo.StaffVO;

public interface StaffService {

    PageResult<StaffVO> page(Long schoolId, String keyword, Integer personType, Long deptId, int page, int size);

    StaffVO get(Long schoolId, Long id);

    StaffVO create(Long schoolId, StaffSaveDTO dto);

    StaffVO update(Long schoolId, Long id, StaffSaveDTO dto);

    void deactivate(Long schoolId, Long id);
}
