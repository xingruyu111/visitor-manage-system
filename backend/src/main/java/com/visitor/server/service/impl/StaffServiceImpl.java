package com.visitor.server.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.visitor.common.core.PageResult;
import com.visitor.common.enums.PersonTypeEnum;
import com.visitor.common.exception.BizException;
import com.visitor.domain.dto.StaffSaveDTO;
import com.visitor.domain.entity.Department;
import com.visitor.domain.entity.SchoolClass;
import com.visitor.domain.entity.Staff;
import com.visitor.domain.entity.User;
import com.visitor.domain.entity.UserRole;
import com.visitor.domain.vo.StaffVO;
import com.visitor.server.mapper.DepartmentMapper;
import com.visitor.server.mapper.SchoolClassMapper;
import com.visitor.server.mapper.StaffMapper;
import com.visitor.server.mapper.UserMapper;
import com.visitor.server.mapper.UserRoleMapper;
import com.visitor.server.service.StaffService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
public class StaffServiceImpl implements StaffService {

    private final StaffMapper staffMapper;
    private final DepartmentMapper departmentMapper;
    private final SchoolClassMapper classMapper;
    private final UserMapper userMapper;
    private final UserRoleMapper userRoleMapper;

    public StaffServiceImpl(StaffMapper staffMapper, DepartmentMapper departmentMapper,
                            SchoolClassMapper classMapper, UserMapper userMapper,
                            UserRoleMapper userRoleMapper) {
        this.staffMapper = staffMapper;
        this.departmentMapper = departmentMapper;
        this.classMapper = classMapper;
        this.userMapper = userMapper;
        this.userRoleMapper = userRoleMapper;
    }

    @Override
    public PageResult<StaffVO> page(Long schoolId, String keyword, Integer personType,
                                    Long deptId, int page, int size) {
        int safePage = Math.max(page, 1);
        int safeSize = Math.min(Math.max(size, 1), 100);

        LambdaQueryWrapper<Staff> wrapper = new LambdaQueryWrapper<Staff>()
                .eq(Staff::getSchoolId, schoolId)
                .eq(personType != null, Staff::getPersonType, PersonTypeEnum.of(personType))
                .eq(deptId != null, Staff::getDeptId, deptId)
                .and(keyword != null && !keyword.isBlank(), w -> w
                        .like(Staff::getName, keyword)
                        .or().like(Staff::getPhone, keyword)
                        .or().like(Staff::getJobNo, keyword)
                        .or().like(Staff::getStudentNo, keyword))
                .orderByDesc(Staff::getId);

        Page<Staff> result = staffMapper.selectPage(new Page<>(safePage, safeSize), wrapper);
        Map<Long, String> deptNames = departmentNames(schoolId);
        Map<Long, String> classNames = classNames(schoolId);

        List<StaffVO> records = result.getRecords().stream()
                .map(staff -> toVO(staff, deptNames, classNames))
                .toList();
        return PageResult.of(result.getTotal(), safePage, safeSize, records);
    }

    @Override
    public StaffVO get(Long schoolId, Long id) {
        Staff staff = require(schoolId, id);
        return toVO(staff, departmentNames(schoolId), classNames(schoolId));
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public StaffVO create(Long schoolId, StaffSaveDTO dto) {
        Staff staff = dto.getPhone() == null ? null : staffMapper.selectOne(new LambdaQueryWrapper<Staff>()
                .eq(Staff::getSchoolId, schoolId)
                .eq(Staff::getPhone, dto.getPhone())
                .eq(Staff::getStatus, 0)
                .last("LIMIT 1"));
        if (staff != null) {
            apply(staff, schoolId, dto);
            staff.setStatus(1);
            staff.setUpdatedAt(LocalDateTime.now());
            staffMapper.updateById(staff);
            restoreAccount(staff);
            return toVO(staff, departmentNames(schoolId), classNames(schoolId));
        }

        staff = new Staff();
        staff.setSchoolId(schoolId);
        staff.setSyncSource("LOCAL");
        staff.setStatus(dto.getStatus() == null ? 1 : dto.getStatus());
        staff.setCreatedAt(LocalDateTime.now());
        apply(staff, schoolId, dto);
        assertPhoneUnique(schoolId, dto.getPhone(), null);
        staffMapper.insert(staff);
        restoreAccount(staff);
        return toVO(staff, departmentNames(schoolId), classNames(schoolId));
    }

    /** 重新添加人员时恢复历史账号与角色。 */
    private void restoreAccount(Staff staff) {
        if (staff.getPhone() == null || staff.getPhone().isBlank()) {
            return;
        }
        User user = userMapper.selectOne(new LambdaQueryWrapper<User>()
                .eq(User::getPhone, staff.getPhone()).last("LIMIT 1"));
        if (user == null) {
            return;
        }
        user.setStatus(1);
        user.setUpdatedAt(LocalDateTime.now());
        userMapper.updateById(user);

        List<UserRole> roles = userRoleMapper.selectList(new LambdaQueryWrapper<UserRole>()
                .eq(UserRole::getUserId, user.getId()));
        if (roles.isEmpty() && staff.getPersonType() != null) {
            UserRole role = new UserRole();
            role.setUserId(user.getId());
            role.setRole(staff.getPersonType().getRole());
            role.setSchoolId(staff.getSchoolId());
            role.setIsDefault(1);
            role.setStatus(1);
            role.setCreatedAt(LocalDateTime.now());
            role.setUpdatedAt(LocalDateTime.now());
            userRoleMapper.insert(role);
            return;
        }
        for (UserRole role : roles) {
            role.setStatus(1);
            role.setUpdatedAt(LocalDateTime.now());
            userRoleMapper.updateById(role);
        }
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public StaffVO update(Long schoolId, Long id, StaffSaveDTO dto) {
        Staff staff = require(schoolId, id);
        apply(staff, schoolId, dto);
        if (dto.getStatus() != null) {
            staff.setStatus(dto.getStatus());
        }
        assertPhoneUnique(schoolId, dto.getPhone(), id);
        staff.setUpdatedAt(LocalDateTime.now());
        staffMapper.updateById(staff);
        return toVO(staff, departmentNames(schoolId), classNames(schoolId));
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void deactivate(Long schoolId, Long id) {
        Staff staff = require(schoolId, id);
        staff.setStatus(0);
        staff.setUpdatedAt(LocalDateTime.now());
        staffMapper.updateById(staff);

        if (staff.getPhone() != null && !staff.getPhone().isBlank()) {
            User user = userMapper.selectOne(new LambdaQueryWrapper<User>()
                    .eq(User::getPhone, staff.getPhone()));
            if (user != null) {
                UserRole disable = new UserRole();
                disable.setStatus(0);
                disable.setUpdatedAt(LocalDateTime.now());
                userRoleMapper.update(disable, new LambdaQueryWrapper<UserRole>()
                        .eq(UserRole::getUserId, user.getId())
                        .eq(UserRole::getStatus, 1));
            }
        }
    }

    private void apply(Staff staff, Long schoolId, StaffSaveDTO dto) {
        staff.setName(dto.getName().trim());
        staff.setPhone(trimToNull(dto.getPhone()));
        PersonTypeEnum personType = PersonTypeEnum.of(dto.getPersonType());
        if (personType == null) {
            throw new BizException(400, "人员类型不合法");
        }
        staff.setPersonType(personType);
        staff.setDeptId(validateDepartment(schoolId, dto.getDeptId()));
        staff.setClassId(validateClass(schoolId, dto.getClassId()));
        staff.setJobNo(trimToNull(dto.getJobNo()));
        staff.setStudentNo(trimToNull(dto.getStudentNo()));
        staff.setPosition(trimToNull(dto.getPosition()));
        staff.setCounselorId(dto.getCounselorId());
    }

    private Long validateDepartment(Long schoolId, Long deptId) {
        if (deptId == null) {
            return null;
        }
        Department department = departmentMapper.selectById(deptId);
        if (department == null || !Objects.equals(department.getSchoolId(), schoolId)) {
            throw new BizException(400, "部门不存在");
        }
        return deptId;
    }

    private Long validateClass(Long schoolId, Long classId) {
        if (classId == null) {
            return null;
        }
        SchoolClass schoolClass = classMapper.selectById(classId);
        if (schoolClass == null || !Objects.equals(schoolClass.getSchoolId(), schoolId)) {
            throw new BizException(400, "班级不存在");
        }
        return classId;
    }

    private void assertPhoneUnique(Long schoolId, String phone, Long excludeId) {
        if (phone == null || phone.isBlank()) {
            return;
        }
        Long count = staffMapper.selectCount(new LambdaQueryWrapper<Staff>()
                .eq(Staff::getSchoolId, schoolId)
                .eq(Staff::getPhone, phone.trim())
                .ne(excludeId != null, Staff::getId, excludeId));
        if (count != null && count > 0) {
            throw new BizException(400, "手机号已存在");
        }
    }

    private Staff require(Long schoolId, Long id) {
        Staff staff = staffMapper.selectById(id);
        if (staff == null || !Objects.equals(staff.getSchoolId(), schoolId)) {
            throw new BizException(404, "人员不存在");
        }
        return staff;
    }

    private Map<Long, String> departmentNames(Long schoolId) {
        return departmentMapper.selectList(new LambdaQueryWrapper<Department>()
                        .eq(Department::getSchoolId, schoolId))
                .stream().collect(Collectors.toMap(Department::getId, Department::getName, (a, b) -> a));
    }

    private Map<Long, String> classNames(Long schoolId) {
        return classMapper.selectList(new LambdaQueryWrapper<SchoolClass>()
                        .eq(SchoolClass::getSchoolId, schoolId))
                .stream().collect(Collectors.toMap(SchoolClass::getId, SchoolClass::getName, (a, b) -> a));
    }

    private StaffVO toVO(Staff staff, Map<Long, String> deptNames, Map<Long, String> classNames) {
        StaffVO vo = new StaffVO();
        vo.setId(staff.getId());
        vo.setSchoolId(staff.getSchoolId());
        vo.setDeptId(staff.getDeptId());
        vo.setDeptName(staff.getDeptId() == null ? null : deptNames.get(staff.getDeptId()));
        vo.setClassId(staff.getClassId());
        vo.setClassName(staff.getClassId() == null ? null : classNames.get(staff.getClassId()));
        vo.setName(staff.getName());
        vo.setPhone(staff.getPhone());
        vo.setPersonType(staff.getPersonType() == null ? null : staff.getPersonType().getCode());
        vo.setPersonTypeDesc(staff.getPersonType() == null ? null : staff.getPersonType().getDescription());
        vo.setJobNo(staff.getJobNo());
        vo.setStudentNo(staff.getStudentNo());
        vo.setPosition(staff.getPosition());
        vo.setCounselorId(staff.getCounselorId());
        vo.setStatus(staff.getStatus());
        vo.setSyncSource(staff.getSyncSource());
        vo.setExternalId(staff.getExternalId());
        return vo;
    }

    private String trimToNull(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }
}
