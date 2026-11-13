package com.visitor.server.service;

import com.visitor.common.core.PageResult;
import com.visitor.domain.dto.WhitelistImportRow;
import com.visitor.domain.dto.WhitelistSaveDTO;
import com.visitor.domain.entity.AccessList;
import org.springframework.web.multipart.MultipartFile;

import java.util.Map;

public interface WhitelistService {

    PageResult<AccessList> page(String keyword, int page, int size);

    PageResult<AccessList> importedPage(int page, int size);

    AccessList create(WhitelistSaveDTO dto);

    AccessList update(Long id, WhitelistSaveDTO dto);

    AccessList approve(Long id);

    AccessList reject(Long id, String reason);

    AccessList updateStatus(Long id, Integer status);

    void delete(Long id);

    Map<String, Object> importExcel(MultipartFile file);

    Map<String, Object> importRows(java.util.List<WhitelistImportRow> rows);
}
