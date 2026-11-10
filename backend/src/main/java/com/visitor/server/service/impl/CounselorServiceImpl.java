package com.visitor.server.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.visitor.common.core.PageResult;
import com.visitor.common.enums.PersonTypeEnum;
import com.visitor.common.enums.RoleEnum;
import com.visitor.common.exception.BizException;
import com.visitor.domain.dto.CounselorOrgSaveDTO;
import com.visitor.domain.entity.CounselorOrg;
import com.visitor.domain.entity.Org;
import com.visitor.domain.entity.Staff;
import com.visitor.domain.entity.User;
import com.visitor.domain.entity.UserRole;
import com.visitor.domain.vo.CounselorOrgVO;
import com.visitor.security.UserContext;
import com.visitor.server.mapper.CounselorOrgMapper;
import com.visitor.server.mapper.OrgMapper;
import com.visitor.server.mapper.StaffMapper;
import com.visitor.server.mapper.UserMapper;
import com.visitor.server.mapper.UserRoleMapper;
import com.visitor.server.service.CounselorService;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Service
public class CounselorServiceImpl implements CounselorService {

    private static final long SCHOOL_ID = 1L;
    private static final String SOURCE_AUTO = "AUTO_COUNSELOR";

    private final CounselorOrgMapper counselorOrgMapper;
    private final StaffMapper staffMapper;
    private final OrgMapper orgMapper;
    private final UserMapper userMapper;
    private final UserRoleMapper userRoleMapper;
    private final PasswordEncoder passwordEncoder;

    public CounselorServiceImpl(CounselorOrgMapper counselorOrgMapper, StaffMapper staffMapper,
                                OrgMapper orgMapper, UserMapper userMapper,
                                UserRoleMapper userRoleMapper, PasswordEncoder passwordEncoder) {
        this.counselorOrgMapper = counselorOrgMapper;
        this.staffMapper = staffMapper;
        this.orgMapper = orgMapper;
        this.userMapper = userMapper;
        this.userRoleMapper = userRoleMapper;
        this.passwordEncoder = passwordEncoder;
    }

    @Override
    public PageResult<CounselorOrgVO> page(String keyword, int page, int size) {
        int safePage = Math.max(page, 1);
        int safeSize = Math.min(Math.max(size, 1), 100);
        Map<Long, String> orgPaths = orgPaths();
        List<CounselorOrgVO> all = new ArrayList<>();
        for (CounselorOrg mapping : counselorOrgMapper.selectList(
                new LambdaQueryWrapper<CounselorOrg>().orderByDesc(CounselorOrg::getId))) {
            CounselorOrgVO vo = toVO(mapping, orgPaths);
            if (keyword != null && !keyword.isBlank()) {
                String text = (vo.getCounselorName() + " " + vo.getCounselorPersonNo() + " "
                        + vo.getCounselorPhone() + " " + vo.getOrgName()).toLowerCase();
                if (!text.contains(keyword.trim().toLowerCase())) {
                    continue;
                }
            }
            all.add(vo);
        }
        int from = Math.min((safePage - 1) * safeSize, all.size());
        int to = Math.min(from + safeSize, all.size());
        return PageResult.of(all.size(), safePage, safeSize, all.subList(from, to));
    }

    @Override
    public List<Map<String, Object>> candidates(String keyword) {
        LambdaQueryWrapper<Staff> wrapper = new LambdaQueryWrapper<Staff>()
                .in(Staff::getPersonType, PersonTypeEnum.TEACHER, PersonTypeEnum.AFFILIATED_TEACHER)
                .eq(Staff::getStatus, 1)
                .eq(Staff::getIsDeleted, 0)
                .isNotNull(Staff::getPhone)
                .ne(Staff::getPhone, "")
                .and(keyword != null && !keyword.isBlank(), w -> w
                        .like(Staff::getName, keyword)
                        .or().like(Staff::getPersonNo, keyword)
                        .or().like(Staff::getPhone, keyword))
                .orderByAsc(Staff::getId)
                .last("LIMIT 20");
        Map<Long, String> orgPaths = orgPaths();
        List<Map<String, Object>> result = new ArrayList<>();
        for (Staff staff : staffMapper.selectList(wrapper)) {
            Map<String, Object> item = new LinkedHashMap<>();
            item.put("id", staff.getId());
            item.put("name", staff.getName());
            item.put("personNo", staff.getPersonNo());
            item.put("phone", staff.getPhone());
            item.put("orgName", staff.getOrgId() == null ? null : orgPaths.get(staff.getOrgId()));
            result.add(item);
        }
        return result;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public CounselorOrgVO create(CounselorOrgSaveDTO dto) {
        Staff counselor = requireCounselor(dto.getCounselorStaffId());
        Org org = orgMapper.selectById(dto.getOrgId());
        if (org == null) {
            throw new BizException(400, "组织不存在");
        }
        Long exists = counselorOrgMapper.selectCount(new LambdaQueryWrapper<CounselorOrg>()
                .eq(CounselorOrg::getSchoolId, SCHOOL_ID)
                .eq(CounselorOrg::getOrgId, dto.getOrgId()));
        if (exists != null && exists > 0) {
            throw new BizException(409, "该组织已配置辅导员");
        }

        User user = ensureAccount(counselor);
        ensureRole(user.getId());

        CounselorOrg mapping = new CounselorOrg();
        mapping.setSchoolId(SCHOOL_ID);
        mapping.setCounselorStaffId(counselor.getId());
        mapping.setOrgId(org.getId());
        mapping.setCreatedBy(UserContext.getUserId());
        mapping.setCreatedAt(LocalDateTime.now());
        mapping.setUpdatedAt(LocalDateTime.now());
        counselorOrgMapper.insert(mapping);
        return toVO(mapping, orgPaths());
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void delete(Long id) {
        CounselorOrg mapping = requireMapping(id);
        counselorOrgMapper.deleteById(id);
        Long remaining = counselorOrgMapper.selectCount(new LambdaQueryWrapper<CounselorOrg>()
                .eq(CounselorOrg::getCounselorStaffId, mapping.getCounselorStaffId()));
        if (remaining == null || remaining == 0) {
            Staff counselor = staffMapper.selectById(mapping.getCounselorStaffId());
            if (counselor != null && counselor.getPhone() != null) {
                User user = userMapper.selectOne(new LambdaQueryWrapper<User>()
                        .eq(User::getPhone, counselor.getPhone()).last("LIMIT 1"));
                if (user != null) {
                    userRoleMapper.delete(new LambdaQueryWrapper<UserRole>()
                            .eq(UserRole::getUserId, user.getId())
                            .eq(UserRole::getRole, RoleEnum.COUNSELOR)
                            .eq(UserRole::getSource, SOURCE_AUTO));
                }
            }
        }
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public CounselorOrgVO updateOrg(Long id, Long orgId) {
        CounselorOrg mapping = requireMapping(id);
        Org org = orgMapper.selectById(orgId);
        if (org == null) {
            throw new BizException(400, "组织不存在");
        }
        Long exists = counselorOrgMapper.selectCount(new LambdaQueryWrapper<CounselorOrg>()
                .eq(CounselorOrg::getSchoolId, SCHOOL_ID)
                .eq(CounselorOrg::getOrgId, orgId)
                .ne(CounselorOrg::getId, id));
        if (exists != null && exists > 0) {
            throw new BizException(409, "该组织已配置辅导员");
        }
        mapping.setOrgId(orgId);
        mapping.setUpdatedAt(LocalDateTime.now());
        counselorOrgMapper.updateById(mapping);
        return toVO(mapping, orgPaths());
    }

    @Override
    public Map<String, Object> preview(Long orgId) {
        if (orgMapper.selectById(orgId) == null) {
            throw new BizException(400, "组织不存在");
        }
        List<Long> ids = descendantOrgIds(orgId);
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("studentCount", countStaff(ids, PersonTypeEnum.STUDENT));
        result.put("staffCount", countStaff(ids, null));
        return result;
    }

    private Map<Long, String> orgPaths() {
        List<Org> all = orgMapper.selectList(null);
        Map<Long, Org> byId = new HashMap<>();
        for (Org org : all) {
            byId.put(org.getId(), org);
        }
        Map<Long, String> cache = new HashMap<>();
        for (Org org : all) {
            path(org, byId, cache);
        }
        return cache;
    }

    private String path(Org org, Map<Long, Org> byId, Map<Long, String> cache) {
        String cached = cache.get(org.getId());
        if (cached != null) {
            return cached;
        }
        String value;
        if (org.getParentId() == null) {
            value = org.getName();
        } else {
            Org parent = byId.get(org.getParentId());
            value = (parent == null ? "" : path(parent, byId, cache) + "/") + org.getName();
        }
        cache.put(org.getId(), value);
        return value;
    }

    private CounselorOrgVO toVO(CounselorOrg mapping, Map<Long, String> orgPaths) {
        CounselorOrgVO vo = new CounselorOrgVO();
        vo.setId(mapping.getId());
        vo.setCounselorStaffId(mapping.getCounselorStaffId());
        vo.setOrgId(mapping.getOrgId());
        vo.setOrgName(orgPaths.get(mapping.getOrgId()));
        vo.setCreatedAt(mapping.getCreatedAt());
        Staff counselor = staffMapper.selectById(mapping.getCounselorStaffId());
        if (counselor != null) {
            vo.setCounselorName(counselor.getName());
            vo.setCounselorPersonNo(counselor.getPersonNo());
            vo.setCounselorPhone(counselor.getPhone());
        }
        List<Long> ids = descendantOrgIds(mapping.getOrgId());
        vo.setStudentCount(countStaff(ids, PersonTypeEnum.STUDENT));
        return vo;
    }

    private Long countStaff(List<Long> orgIds, PersonTypeEnum type) {
        if (orgIds.isEmpty()) {
            return 0L;
        }
        Long count = staffMapper.selectCount(new LambdaQueryWrapper<Staff>()
                .in(Staff::getOrgId, orgIds)
                .eq(Staff::getIsDeleted, 0)
                .eq(type != null, Staff::getPersonType, type));
        return count == null ? 0L : count;
    }

    private List<Long> descendantOrgIds(Long rootId) {
        List<Long> ids = new ArrayList<>();
        for (Org org : orgMapper.selectList(null)) {
            if (org.getId().equals(rootId)) {
                ids.add(org.getId());
                continue;
            }
            if (org.getAncestors() != null) {
                for (String part : org.getAncestors().split(",")) {
                    if (part.equals(String.valueOf(rootId))) {
                        ids.add(org.getId());
                        break;
                    }
                }
            }
        }
        return ids;
    }

    private Staff requireCounselor(Long staffId) {
        Staff staff = staffMapper.selectById(staffId);
        if (staff == null || Integer.valueOf(1).equals(staff.getIsDeleted())) {
            throw new BizException(400, "人员不存在");
        }
        if (staff.getPersonType() != PersonTypeEnum.TEACHER
                && staff.getPersonType() != PersonTypeEnum.AFFILIATED_TEACHER) {
            throw new BizException(400, "只能选择教职工担任辅导员");
        }
        if (!Integer.valueOf(1).equals(staff.getStatus())) {
            throw new BizException(400, "该人员已停用");
        }
        if (staff.getPhone() == null || staff.getPhone().isBlank()) {
            throw new BizException(400, "该人员没有手机号，无法创建审批账号");
        }
        return staff;
    }

    private User ensureAccount(Staff staff) {
        User user = userMapper.selectOne(new LambdaQueryWrapper<User>()
                .eq(User::getPhone, staff.getPhone()).last("LIMIT 1"));
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
            if (!Integer.valueOf(1).equals(user.getStatus())) {
                throw new BizException(400, "该人员账号已停用");
            }
            if (user.getPersonNo() == null) {
                user.setPersonNo(staff.getPersonNo());
                user.setUpdatedAt(LocalDateTime.now());
                userMapper.updateById(user);
            }
        }
        return user;
    }

    private void ensureRole(Long userId) {
        Long exists = userRoleMapper.selectCount(new LambdaQueryWrapper<UserRole>()
                .eq(UserRole::getUserId, userId)
                .eq(UserRole::getRole, RoleEnum.COUNSELOR));
        if (exists != null && exists > 0) {
            return;
        }
        UserRole role = new UserRole();
        role.setUserId(userId);
        role.setRole(RoleEnum.COUNSELOR);
        role.setIsDefault(0);
        role.setStatus(1);
        role.setSource(SOURCE_AUTO);
        role.setCreatedAt(LocalDateTime.now());
        role.setUpdatedAt(LocalDateTime.now());
        userRoleMapper.insert(role);
    }

    private CounselorOrg requireMapping(Long id) {
        CounselorOrg mapping = counselorOrgMapper.selectById(id);
        if (mapping == null) {
            throw new BizException(404, "辅导员关联不存在");
        }
        return mapping;
    }
}
