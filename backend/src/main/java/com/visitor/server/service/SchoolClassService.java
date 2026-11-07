package com.visitor.server.service;

import com.visitor.domain.dto.SchoolClassSaveDTO;
import com.visitor.domain.vo.SchoolClassVO;

import java.util.List;

public interface SchoolClassService {

    List<SchoolClassVO> list(Long schoolId);

    SchoolClassVO create(Long schoolId, SchoolClassSaveDTO dto);

    SchoolClassVO update(Long schoolId, Long id, SchoolClassSaveDTO dto);
}
