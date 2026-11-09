package com.visitor.server.service;

import com.visitor.domain.entity.Org;

import java.util.List;
import java.util.Map;

public interface OrgService {

    List<Map<String, Object>> tree();

    Org create(Long parentId, String name, Integer sortOrder);

    Org update(Long id, Long parentId, String name, Integer sortOrder);

    void delete(Long id);

    java.util.Map<String, Object> importRows(java.util.List<com.visitor.domain.dto.OrgImportRow> rows);
}
