package com.visitor.server.service;

import com.visitor.domain.dto.AccessListImportRow;
import com.visitor.domain.dto.VisitorRecordImportRow;

import java.util.List;
import java.util.Map;

public interface MigrationService {

    Map<String, Object> importVisitorRecords(List<VisitorRecordImportRow> rows);

    Map<String, Object> importAccessList(List<AccessListImportRow> rows);
}
