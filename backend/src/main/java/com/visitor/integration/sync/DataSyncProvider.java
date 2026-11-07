package com.visitor.integration.sync;

import java.util.List;

/**
 * 数据中台同步：拉取组织、教职工、学生、校友数据，由业务层落库。
 */
public interface DataSyncProvider {

    String protocol();

    boolean isConfigured();

    List<OrgUnit> fetchOrg();

    List<PersonRecord> fetchStaff();

    List<PersonRecord> fetchStudent();

    List<PersonRecord> fetchAlumni();

    record OrgUnit(String externalId, String parentExternalId, String name, Integer sortOrder) {
    }

    record PersonRecord(String externalId, String name, String phone, String personType,
                        String jobNo, String studentNo, String deptExternalId,
                        String className, String grade) {
    }
}
