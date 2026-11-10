package com.visitor.server.service;

import com.visitor.common.core.PageResult;
import com.visitor.domain.dto.CounselorOrgSaveDTO;
import com.visitor.domain.vo.CounselorOrgVO;

import java.util.List;
import java.util.Map;

public interface CounselorService {

    PageResult<CounselorOrgVO> page(String keyword, int page, int size);

    List<Map<String, Object>> candidates(String keyword);

    CounselorOrgVO create(CounselorOrgSaveDTO dto);

    void delete(Long id);

    CounselorOrgVO updateOrg(Long id, Long orgId);

    Map<String, Object> preview(Long orgId);
}
