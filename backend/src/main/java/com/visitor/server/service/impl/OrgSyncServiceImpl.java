package com.visitor.server.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.visitor.common.enums.PersonTypeEnum;
import com.visitor.common.exception.BizException;
import com.visitor.domain.entity.Department;
import com.visitor.domain.entity.SchoolClass;
import com.visitor.domain.entity.Staff;
import com.visitor.domain.entity.User;
import com.visitor.domain.entity.UserRole;
import com.visitor.integration.IntegrationFactory;
import com.visitor.integration.sync.DataSyncProvider;
import com.visitor.server.mapper.DepartmentMapper;
import com.visitor.server.mapper.SchoolClassMapper;
import com.visitor.server.mapper.StaffMapper;
import com.visitor.server.mapper.UserMapper;
import com.visitor.server.mapper.UserRoleMapper;
import com.visitor.server.service.StaffService;
import com.visitor.server.service.OrgSyncService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

@Service
public class OrgSyncServiceImpl implements OrgSyncService {

    private final IntegrationFactory integrationFactory;
    private final DepartmentMapper departmentMapper;
    private final SchoolClassMapper classMapper;
    private final StaffMapper staffMapper;
    private final UserMapper userMapper;
    private final UserRoleMapper userRoleMapper;
    private final StaffService staffService;

    public OrgSyncServiceImpl(IntegrationFactory integrationFactory, DepartmentMapper departmentMapper,
                              SchoolClassMapper classMapper, StaffMapper staffMapper,
                              UserMapper userMapper, UserRoleMapper userRoleMapper,
                              StaffService staffService) {
        this.integrationFactory = integrationFactory;
        this.departmentMapper = departmentMapper;
        this.classMapper = classMapper;
        this.staffMapper = staffMapper;
        this.userMapper = userMapper;
        this.userRoleMapper = userRoleMapper;
        this.staffService = staffService;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public SyncResult sync(Long schoolId, String type) {
        DataSyncProvider provider = integrationFactory.dataSync();
        if (!provider.isConfigured()) {
            throw new BizException(400, "数据中台未配置");
        }
        String normalized = type == null ? "" : type.trim().toLowerCase();
        return switch (normalized) {
            case "org" -> syncOrg(schoolId, provider.fetchOrg());
            case "staff" -> syncPersons(schoolId, "staff", provider.fetchStaff());
            case "student" -> syncPersons(schoolId, "student", provider.fetchStudent());
            case "alumni" -> syncPersons(schoolId, "alumni", provider.fetchAlumni());
            default -> throw new BizException(400, "不支持的同步类型: " + type);
        };
    }

    private SyncResult syncOrg(Long schoolId, List<DataSyncProvider.OrgUnit> units) {
        Map<String, Department> byExternal = departmentMapper.selectList(
                        new LambdaQueryWrapper<Department>().eq(Department::getSchoolId, schoolId))
                .stream()
                .filter(department -> department.getExternalId() != null)
                .collect(Collectors.toMap(Department::getExternalId, department -> department, (a, b) -> a));

        int created = 0;
        int updated = 0;
        for (DataSyncProvider.OrgUnit unit : units) {
            Department department = byExternal.get(unit.externalId());
            if (department == null) {
                department = new Department();
                department.setSchoolId(schoolId);
                department.setExternalId(unit.externalId());
                department.setName(unit.name());
                department.setSortOrder(unit.sortOrder() == null ? 0 : unit.sortOrder());
                department.setLevel(1);
                department.setAncestors("");
                department.setStatus(1);
                department.setCreatedAt(LocalDateTime.now());
                department.setUpdatedAt(LocalDateTime.now());
                departmentMapper.insert(department);
                byExternal.put(unit.externalId(), department);
                created++;
            } else {
                department.setName(unit.name());
                if (unit.sortOrder() != null) {
                    department.setSortOrder(unit.sortOrder());
                }
                department.setUpdatedAt(LocalDateTime.now());
                departmentMapper.updateById(department);
                updated++;
            }
        }

        for (DataSyncProvider.OrgUnit unit : units) {
            if (unit.parentExternalId() == null) {
                continue;
            }
            Department department = byExternal.get(unit.externalId());
            Department parent = byExternal.get(unit.parentExternalId());
            if (department == null || parent == null) {
                continue;
            }
            department.setParentId(parent.getId());
            department.setLevel((parent.getLevel() == null ? 1 : parent.getLevel()) + 1);
            String ancestors = parent.getAncestors();
            department.setAncestors((ancestors == null || ancestors.isBlank() ? "" : ancestors + ",") + parent.getId());
            department.setUpdatedAt(LocalDateTime.now());
            departmentMapper.updateById(department);
        }
        return new SyncResult("org", created, updated, 0, 0);
    }

    private SyncResult syncPersons(Long schoolId, String type, List<DataSyncProvider.PersonRecord> records) {
        Map<String, Long> deptIds = departmentMapper.selectList(
                        new LambdaQueryWrapper<Department>().eq(Department::getSchoolId, schoolId))
                .stream()
                .filter(department -> department.getExternalId() != null)
                .collect(Collectors.toMap(Department::getExternalId, Department::getId, (a, b) -> a));

        Map<String, Long> classIds = classMapper.selectList(
                        new LambdaQueryWrapper<SchoolClass>().eq(SchoolClass::getSchoolId, schoolId))
                .stream()
                .collect(Collectors.toMap(SchoolClass::getName, SchoolClass::getId, (a, b) -> a, HashMap::new));

        Map<String, Staff> existing = staffMapper.selectList(
                        new LambdaQueryWrapper<Staff>().eq(Staff::getSchoolId, schoolId))
                .stream()
                .filter(staff -> staff.getExternalId() != null)
                .collect(Collectors.toMap(Staff::getExternalId, staff -> staff, (a, b) -> a));

        int created = 0;
        int updated = 0;
        int failed = 0;
        Set<String> seen = new HashSet<>();

        for (DataSyncProvider.PersonRecord record : records) {
            try {
                PersonTypeEnum personType = PersonTypeEnum.of(record.personType());
                if (personType == null || record.externalId() == null) {
                    failed++;
                    continue;
                }
                Long deptId = record.deptExternalId() == null ? null : deptIds.get(record.deptExternalId());
                Long classId = record.className() == null ? null
                        : classIds.computeIfAbsent(record.className(),
                        name -> createClass(schoolId, name, record.grade()));

                Staff staff = existing.get(record.externalId());
                if (staff == null) {
                    staff = new Staff();
                    staff.setSchoolId(schoolId);
                    staff.setExternalId(record.externalId());
                    staff.setCreatedAt(LocalDateTime.now());
                    staff.setSyncSource("SYNC");
                    staff.setStatus(1);
                    fill(staff, record, personType, deptId, classId);
                    staffMapper.insert(staff);
                    existing.put(record.externalId(), staff);
                    ensureAccount(schoolId, staff);
                    created++;
                } else {
                    fill(staff, record, personType, deptId, classId);
                    staff.setStatus(1);
                    staff.setSyncSource("SYNC");
                    staff.setUpdatedAt(LocalDateTime.now());
                    staffMapper.updateById(staff);
                    ensureAccount(schoolId, staff);
                    updated++;
                }
                seen.add(record.externalId());
            } catch (Exception e) {
                failed++;
            }
        }

        int deactivated = deactivateMissing(schoolId, records, existing, seen);
        return new SyncResult(type, created, updated, deactivated, failed);
    }

    private int deactivateMissing(Long schoolId, List<DataSyncProvider.PersonRecord> records,
                                  Map<String, Staff> existing, Set<String> seen) {
        Set<PersonTypeEnum> types = records.stream()
                .map(record -> PersonTypeEnum.of(record.personType()))
                .filter(Objects::nonNull)
                .collect(Collectors.toSet());
        if (types.isEmpty()) {
            return 0;
        }
        int deactivated = 0;
        for (Staff staff : existing.values()) {
            if (staff.getPersonType() != null && types.contains(staff.getPersonType())
                    && !seen.contains(staff.getExternalId())
                    && Integer.valueOf(1).equals(staff.getStatus())) {
                staff.setStatus(0);
                staff.setUpdatedAt(LocalDateTime.now());
                staffMapper.updateById(staff);
                revokeRoles(staff.getPhone());
                deactivated++;
            }
        }
        return deactivated;
    }

    private void fill(Staff staff, DataSyncProvider.PersonRecord record, PersonTypeEnum personType,
                      Long deptId, Long classId) {
        staff.setName(record.name());
        staff.setPhone(record.phone());
        staff.setPersonType(personType);
        staff.setPersonNo(firstNonBlank(record.jobNo(), record.studentNo(), record.externalId()));
        staff.setJobNo(record.jobNo());
        staff.setStudentNo(record.studentNo());
        staff.setDeptId(deptId);
        staff.setClassId(classId);
    }

    private Long createClass(Long schoolId, String name, String grade) {
        SchoolClass schoolClass = new SchoolClass();
        schoolClass.setSchoolId(schoolId);
        schoolClass.setName(name);
        schoolClass.setGrade(grade);
        schoolClass.setStatus(1);
        schoolClass.setCreatedAt(LocalDateTime.now());
        schoolClass.setUpdatedAt(LocalDateTime.now());
        classMapper.insert(schoolClass);
        return schoolClass.getId();
    }

    private void ensureAccount(Long schoolId, Staff staff) {
        if (staff.getPhone() == null || staff.getPhone().isBlank()) {
            return;
        }
        try {
            staffService.ensureAccountForStaff(schoolId, staff.getId());
        } catch (Exception ignored) {
            // 账号冲突不阻断人员同步，由管理员在账号管理中处理
        }
    }

    private String firstNonBlank(String... values) {
        for (String value : values) {
            if (value != null && !value.isBlank()) {
                return value;
            }
        }
        return null;
    }

    private void revokeRoles(String phone) {
        if (phone == null || phone.isBlank()) {
            return;
        }
        User user = userMapper.selectOne(new LambdaQueryWrapper<User>().eq(User::getPhone, phone));
        if (user == null) {
            return;
        }
        user.setStatus(0);
        user.setUpdatedAt(LocalDateTime.now());
        userMapper.updateById(user);
    }
}
