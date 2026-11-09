package com.visitor.server.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.visitor.common.core.PageResult;
import com.visitor.common.enums.PersonTypeEnum;
import com.visitor.common.enums.RoleEnum;
import com.visitor.common.exception.BizException;
import com.visitor.domain.dto.StaffSaveDTO;
import com.visitor.domain.entity.ApprovalTask;
import com.visitor.domain.entity.Department;
import com.visitor.domain.entity.Org;
import com.visitor.domain.entity.SchoolClass;
import com.visitor.domain.entity.Staff;
import com.visitor.domain.entity.User;
import com.visitor.domain.entity.UserRole;
import com.visitor.domain.vo.StaffVO;
import com.visitor.server.mapper.ApprovalTaskMapper;
import com.visitor.server.mapper.DepartmentMapper;
import com.visitor.server.mapper.OrgMapper;
import com.visitor.server.mapper.SchoolClassMapper;
import com.visitor.server.mapper.StaffMapper;
import com.visitor.server.mapper.UserMapper;
import com.visitor.server.mapper.UserRoleMapper;
import com.visitor.server.service.StaffService;
import com.visitor.workflow.ApprovalStatuses;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
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
    private final OrgMapper orgMapper;
    private final SchoolClassMapper classMapper;
    private final UserMapper userMapper;
    private final UserRoleMapper userRoleMapper;
    private final ApprovalTaskMapper taskMapper;
    private final PasswordEncoder passwordEncoder;

    public StaffServiceImpl(StaffMapper staffMapper, DepartmentMapper departmentMapper,
                            OrgMapper orgMapper, SchoolClassMapper classMapper, UserMapper userMapper,
                            UserRoleMapper userRoleMapper, ApprovalTaskMapper taskMapper,
                            PasswordEncoder passwordEncoder) {
        this.staffMapper = staffMapper;
        this.departmentMapper = departmentMapper;
        this.orgMapper = orgMapper;
        this.classMapper = classMapper;
        this.userMapper = userMapper;
        this.userRoleMapper = userRoleMapper;
        this.taskMapper = taskMapper;
        this.passwordEncoder = passwordEncoder;
    }

    @Override
    public PageResult<StaffVO> page(Long schoolId, String keyword, Integer personType,
                                    Long deptId, int page, int size) {
        int safePage = Math.max(page, 1);
        int safeSize = Math.min(Math.max(size, 1), 100);

        LambdaQueryWrapper<Staff> wrapper = new LambdaQueryWrapper<Staff>()
                .eq(Staff::getSchoolId, schoolId)
                .eq(Staff::getIsDeleted, 0)
                .eq(personType != null, Staff::getPersonType, PersonTypeEnum.of(personType))
                .eq(deptId != null, Staff::getDeptId, deptId)
                .and(keyword != null && !keyword.isBlank(), w -> w
                        .like(Staff::getName, keyword)
                        .or().like(Staff::getPhone, keyword)
                        .or().like(Staff::getPersonNo, keyword)
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
        Staff staff = dto.getPersonNo() == null ? null : staffMapper.selectOne(new LambdaQueryWrapper<Staff>()
                .eq(Staff::getSchoolId, schoolId)
                .eq(Staff::getPersonNo, dto.getPersonNo().trim())
                .last("LIMIT 1"));
        if (staff != null) {
            if (Integer.valueOf(1).equals(staff.getStatus())) {
                throw new BizException(400, "工号/学号已存在");
            }
            apply(staff, schoolId, dto);
            staff.setStatus(1);
            staff.setIsDeleted(0);
            staff.setUpdatedAt(LocalDateTime.now());
            assertPhoneUnique(schoolId, dto.getPhone(), staff.getId());
            staffMapper.updateById(staff);
            User user = ensureAccount(staff);
            restoreRoles(user.getId());
            return toVO(staff, departmentNames(schoolId), classNames(schoolId));
        }

        staff = new Staff();
        staff.setSchoolId(schoolId);
        staff.setSyncSource("LOCAL");
        staff.setStatus(dto.getStatus() == null ? 1 : dto.getStatus());
        staff.setIsDeleted(0);
        staff.setCreatedAt(LocalDateTime.now());
        apply(staff, schoolId, dto);
        assertPersonNoUnique(schoolId, dto.getPersonNo(), null);
        assertPhoneUnique(schoolId, dto.getPhone(), null);
        staffMapper.insert(staff);
        if (Integer.valueOf(1).equals(staff.getStatus())) {
            ensureAccount(staff);
        }
        return toVO(staff, departmentNames(schoolId), classNames(schoolId));
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void ensureAccountForStaff(Long schoolId, Long staffId) {
        Staff staff = require(schoolId, staffId);
        if (Integer.valueOf(1).equals(staff.getStatus())) {
            ensureAccount(staff);
        }
    }

    /** 人员新增/导入后确保账号存在，并写入主角色。 */
    private User ensureAccount(Staff staff) {
        if (staff.getPhone() == null || staff.getPhone().isBlank()) {
            throw new BizException(400, "手机号不能为空");
        }
        User user = staff.getPersonNo() == null ? null : userMapper.selectOne(new LambdaQueryWrapper<User>()
                .eq(User::getPersonNo, staff.getPersonNo()).last("LIMIT 1"));
        if (user == null) {
            user = userMapper.selectOne(new LambdaQueryWrapper<User>()
                    .eq(User::getPhone, staff.getPhone()).last("LIMIT 1"));
            if (user != null && user.getPersonNo() != null
                    && !user.getPersonNo().equals(staff.getPersonNo())) {
                Staff previous = staffMapper.selectOne(new LambdaQueryWrapper<Staff>()
                        .eq(Staff::getPersonNo, user.getPersonNo()).last("LIMIT 1"));
                if (previous == null || !Integer.valueOf(1).equals(previous.getIsDeleted())) {
                    throw new BizException(400, "手机号已绑定其他人员");
                }
                user.setPersonNo(staff.getPersonNo());
            }
        }
        if (user == null) {
            user = new User();
            user.setPhone(staff.getPhone());
            user.setName(staff.getName());
            user.setPersonNo(staff.getPersonNo());
            user.setPassword(passwordEncoder.encode("123456"));
            user.setStatus(1);
            user.setCreatedAt(LocalDateTime.now());
            user.setUpdatedAt(LocalDateTime.now());
            userMapper.insert(user);
        } else {
            user.setStatus(1);
            user.setName(staff.getName());
            if (user.getPersonNo() == null) {
                user.setPersonNo(staff.getPersonNo());
            }
            user.setUpdatedAt(LocalDateTime.now());
            userMapper.updateById(user);
        }
        syncPrimaryRole(staff, user);
        return user;
    }

    /** 按人员类型维护唯一的主角色（source=IDENTITY）。 */
    private void syncPrimaryRole(Staff staff, User user) {
        RoleEnum target = staff.getPersonType().getRole();
        List<UserRole> roles = userRoleMapper.selectList(new LambdaQueryWrapper<UserRole>()
                .eq(UserRole::getUserId, user.getId()));
        UserRole matched = null;
        for (UserRole role : roles) {
            if ("IDENTITY".equals(role.getSource()) && role.getRole() != target) {
                userRoleMapper.deleteById(role.getId());
                continue;
            }
            if (role.getRole() == target) {
                matched = role;
            }
        }
        if (matched != null) {
            matched.setSource("IDENTITY");
            matched.setIsDefault(1);
            matched.setStatus(1);
            matched.setUpdatedAt(LocalDateTime.now());
            userRoleMapper.updateById(matched);
        } else {
            UserRole role = new UserRole();
            role.setUserId(user.getId());
            role.setRole(target);
            role.setSchoolId(staff.getSchoolId());
            role.setIsDefault(1);
            role.setSource("IDENTITY");
            role.setStatus(1);
            role.setCreatedAt(LocalDateTime.now());
            role.setUpdatedAt(LocalDateTime.now());
            userRoleMapper.insert(role);
        }
        for (UserRole role : userRoleMapper.selectList(new LambdaQueryWrapper<UserRole>()
                .eq(UserRole::getUserId, user.getId())
                .ne(UserRole::getRole, target)
                .eq(UserRole::getIsDefault, 1))) {
            role.setIsDefault(0);
            role.setUpdatedAt(LocalDateTime.now());
            userRoleMapper.updateById(role);
        }
    }

    private void restoreRoles(Long userId) {
        List<UserRole> roles = userRoleMapper.selectList(new LambdaQueryWrapper<UserRole>()
                .eq(UserRole::getUserId, userId));
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
        String oldPersonNo = staff.getPersonNo();
        PersonTypeEnum oldType = staff.getPersonType();
        apply(staff, schoolId, dto);
        assertPersonNoUnique(schoolId, dto.getPersonNo(), id);
        assertPhoneUnique(schoolId, dto.getPhone(), id);
        boolean deactivate = Integer.valueOf(0).equals(dto.getStatus());
        if (deactivate) {
            assertNoPending(staff);
        }
        staff.setUpdatedAt(LocalDateTime.now());
        staffMapper.updateById(staff);

        User user = findLinkedUser(staff, oldPersonNo);
        if (user != null) {
            if (deactivate || Integer.valueOf(0).equals(staff.getStatus())) {
                disableAccount(user);
            } else {
                user.setStatus(1);
                user.setName(staff.getName());
                user.setPersonNo(staff.getPersonNo());
                user.setUpdatedAt(LocalDateTime.now());
                userMapper.updateById(user);
                if (oldType != staff.getPersonType()) {
                    syncPrimaryRole(staff, user);
                }
            }
        } else if (!deactivate && Integer.valueOf(1).equals(staff.getStatus())) {
            ensureAccount(staff);
        }
        return toVO(staff, departmentNames(schoolId), classNames(schoolId));
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void deactivate(Long schoolId, Long id) {
        Staff staff = require(schoolId, id);
        User user = assertNoPending(staff);
        staff.setStatus(0);
        staff.setUpdatedAt(LocalDateTime.now());
        staffMapper.updateById(staff);
        if (user != null) {
            disableAccount(user);
        }
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void delete(Long schoolId, Long id) {
        Staff staff = require(schoolId, id);
        User user = assertNoPending(staff);
        staff.setStatus(0);
        staff.setIsDeleted(1);
        staff.setUpdatedAt(LocalDateTime.now());
        staffMapper.updateById(staff);
        if (user != null) {
            disableAccount(user);
        }
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void updateStatus(Long schoolId, Long id, Integer status) {
        Staff staff = require(schoolId, id);
        if (status == null || (status != 0 && status != 1)) {
            throw new BizException(400, "状态不合法");
        }
        if (status == 1) {
            if (Integer.valueOf(1).equals(staff.getIsDeleted())) {
                throw new BizException(400, "人员已删除，请重新添加");
            }
            staff.setStatus(1);
            staff.setUpdatedAt(LocalDateTime.now());
            staffMapper.updateById(staff);
            User user = findLinkedUser(staff, staff.getPersonNo());
            if (user != null) {
                user.setStatus(1);
                user.setUpdatedAt(LocalDateTime.now());
                userMapper.updateById(user);
                restoreRoles(user.getId());
            } else {
                ensureAccount(staff);
            }
            return;
        }
        User user = assertNoPending(staff);
        staff.setStatus(0);
        staff.setUpdatedAt(LocalDateTime.now());
        staffMapper.updateById(staff);
        if (user != null) {
            disableAccount(user);
        }
    }

    /** 待办存在时阻止停用。 */
    private User assertNoPending(Staff staff) {
        User user = findLinkedUser(staff, staff.getPersonNo());
        if (user == null) {
            return null;
        }
        Long pending = taskMapper.selectCount(new LambdaQueryWrapper<ApprovalTask>()
                .eq(ApprovalTask::getAssigneeId, user.getId())
                .eq(ApprovalTask::getStatus, ApprovalStatuses.PENDING));
        if (pending != null && pending > 0) {
            throw new BizException(400, "还有 " + pending + " 条待审批，请先转交");
        }
        return user;
    }

    private User findLinkedUser(Staff staff, String oldPersonNo) {
        User user = staff.getPersonNo() == null ? null : userMapper.selectOne(new LambdaQueryWrapper<User>()
                .eq(User::getPersonNo, staff.getPersonNo()).last("LIMIT 1"));
        if (user == null && oldPersonNo != null) {
            user = userMapper.selectOne(new LambdaQueryWrapper<User>()
                    .eq(User::getPersonNo, oldPersonNo).last("LIMIT 1"));
        }
        if (user == null && staff.getPhone() != null && !staff.getPhone().isBlank()) {
            user = userMapper.selectOne(new LambdaQueryWrapper<User>()
                    .eq(User::getPhone, staff.getPhone()).last("LIMIT 1"));
        }
        return user;
    }

    private void disableAccount(User user) {
        user.setStatus(0);
        user.setUpdatedAt(LocalDateTime.now());
        userMapper.updateById(user);
        UserRole disable = new UserRole();
        disable.setStatus(0);
        disable.setUpdatedAt(LocalDateTime.now());
        userRoleMapper.update(disable, new LambdaQueryWrapper<UserRole>()
                .eq(UserRole::getUserId, user.getId())
                .eq(UserRole::getStatus, 1));
    }

    private void apply(Staff staff, Long schoolId, StaffSaveDTO dto) {
        if (dto.getPersonNo() == null || dto.getPersonNo().isBlank()) {
            throw new BizException(400, "工号/学号不能为空");
        }
        if (dto.getPhone() == null || dto.getPhone().isBlank()) {
            throw new BizException(400, "手机号不能为空");
        }
        staff.setName(dto.getName().trim());
        staff.setPersonNo(dto.getPersonNo().trim());
        staff.setPhone(dto.getPhone().trim());
        PersonTypeEnum personType = PersonTypeEnum.of(dto.getPersonType());
        if (personType == null) {
            throw new BizException(400, "人员类型不合法");
        }
        staff.setPersonType(personType);
        if (dto.getOrgId() != null) {
            Org org = orgMapper.selectById(dto.getOrgId());
            if (org == null) {
                throw new BizException(400, "组织节点不存在");
            }
            staff.setOrgId(org.getId());
            if (org.getExternalId() != null && org.getExternalId().startsWith("DEPT:")) {
                staff.setDeptId(Long.valueOf(org.getExternalId().substring(5)));
            } else if (org.getExternalId() != null && org.getExternalId().startsWith("CLASS:")) {
                staff.setClassId(Long.valueOf(org.getExternalId().substring(6)));
            }
        }
        staff.setDeptId(validateDepartment(schoolId, dto.getDeptId()));
        staff.setClassId(validateClass(schoolId, dto.getClassId()));
        staff.setJobNo(trimToNull(dto.getJobNo()) == null ? staff.getPersonNo() : trimToNull(dto.getJobNo()));
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

    private void assertPersonNoUnique(Long schoolId, String personNo, Long excludeId) {
        if (personNo == null || personNo.isBlank()) {
            throw new BizException(400, "工号/学号不能为空");
        }
        Long count = staffMapper.selectCount(new LambdaQueryWrapper<Staff>()
                .eq(Staff::getSchoolId, schoolId)
                .eq(Staff::getPersonNo, personNo.trim())
                .ne(excludeId != null, Staff::getId, excludeId));
        if (count != null && count > 0) {
            throw new BizException(400, "工号/学号已存在");
        }
    }

    private void assertPhoneUnique(Long schoolId, String phone, Long excludeId) {
        if (phone == null || phone.isBlank()) {
            return;
        }
        Long count = staffMapper.selectCount(new LambdaQueryWrapper<Staff>()
                .eq(Staff::getSchoolId, schoolId)
                .eq(Staff::getIsDeleted, 0)
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
        vo.setOrgId(staff.getOrgId());
        if (staff.getOrgId() != null) {
            Org org = orgMapper.selectById(staff.getOrgId());
            vo.setOrgName(org == null ? null : org.getName());
        }
        vo.setDeptId(staff.getDeptId());
        vo.setDeptName(staff.getDeptId() == null ? null : deptNames.get(staff.getDeptId()));
        vo.setClassId(staff.getClassId());
        vo.setClassName(staff.getClassId() == null ? null : classNames.get(staff.getClassId()));
        vo.setName(staff.getName());
        vo.setPhone(staff.getPhone());
        vo.setPersonNo(staff.getPersonNo());
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
