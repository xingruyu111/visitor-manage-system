package com.visitor.server.service;

import com.visitor.domain.dto.DepartmentSaveDTO;
import com.visitor.domain.vo.DepartmentVO;

import java.util.List;

public interface DepartmentService {

    List<DepartmentVO> tree(Long schoolId);

    DepartmentVO create(Long schoolId, DepartmentSaveDTO dto);

    DepartmentVO update(Long schoolId, Long id, DepartmentSaveDTO dto);

    void delete(Long schoolId, Long id);
}
