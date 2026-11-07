package com.visitor.integration.sync;

import org.springframework.stereotype.Component;

import java.util.List;

/**
 * 联调用 Mock 数据中台：把 vst_integration_config 中 DATA_SYNC 的 protocol 设为 MOCK 即可启用。
 */
@Component
public class MockDataSyncProvider implements DataSyncProvider {

    @Override
    public String protocol() {
        return "MOCK";
    }

    @Override
    public boolean isConfigured() {
        return true;
    }

    @Override
    public List<OrgUnit> fetchOrg() {
        return List.of(
                new OrgUnit("D001", null, "信息工程学院", 1),
                new OrgUnit("D002", "D001", "计算机系", 2)
        );
    }

    @Override
    public List<PersonRecord> fetchStaff() {
        return List.of(
                new PersonRecord("T001", "张老师", "13900001001", "TEACHER", "T001", null, "D002", null, null),
                new PersonRecord("T002", "李老师", "13900001002", "TEACHER", "T002", null, "D002", null, null),
                new PersonRecord("C001", "王师傅", "13900001003", "CANTEEN", "C001", null, null, null, null)
        );
    }

    @Override
    public List<PersonRecord> fetchStudent() {
        return List.of(
                new PersonRecord("S001", "学生甲", "13900002001", "STUDENT", null, "S001", "D002", "计算机2201", "2022"),
                new PersonRecord("S002", "学生乙", "13900002002", "STUDENT", null, "S002", "D002", "计算机2201", "2022")
        );
    }

    @Override
    public List<PersonRecord> fetchAlumni() {
        return List.of(
                new PersonRecord("A001", "校友丙", "13900003001", "ALUMNI", null, null, null, null, null)
        );
    }
}
