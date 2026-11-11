package com.visitor.server.service.impl;

import com.alibaba.excel.EasyExcel;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.visitor.common.core.PageResult;
import com.visitor.common.enums.RoleEnum;
import com.visitor.common.exception.BizException;
import com.visitor.domain.dto.WhitelistImportRow;
import com.visitor.domain.dto.WhitelistSaveDTO;
import com.visitor.domain.entity.AccessList;
import com.visitor.domain.entity.User;
import com.visitor.domain.entity.UserRole;
import com.visitor.domain.entity.VisitorRecord;
import com.visitor.security.RoleAccessChecker;
import com.visitor.security.SchoolScopes;
import com.visitor.security.UserContext;
import com.visitor.server.mapper.AccessListMapper;
import com.visitor.server.mapper.UserMapper;
import com.visitor.server.mapper.UserRoleMapper;
import com.visitor.server.mapper.VisitorRecordMapper;
import com.visitor.server.service.AccessCheckService;
import com.visitor.server.service.NotificationService;
import com.visitor.server.service.WhitelistService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

@Service
public class WhitelistServiceImpl implements WhitelistService {

    private static final int LIST_WHITE = 2;
    private static final int PENDING = 0;
    private static final int ACTIVE = 1;
    private static final int REJECTED = 2;
    private static final int DISABLED = 3;
    private static final DateTimeFormatter FORMAT = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
    private static final DateTimeFormatter DATE = DateTimeFormatter.ofPattern("yyyy-MM-dd");

    private final AccessListMapper accessListMapper;
    private final UserMapper userMapper;
    private final UserRoleMapper userRoleMapper;
    private final VisitorRecordMapper recordMapper;
    private final VisitorPassIssuer passIssuer;
    private final NotificationService notificationService;
    private final RoleAccessChecker roleAccessChecker;
    private final AccessCheckService accessCheckService;

    public WhitelistServiceImpl(AccessListMapper accessListMapper, UserMapper userMapper,
                                UserRoleMapper userRoleMapper, VisitorRecordMapper recordMapper,
                                VisitorPassIssuer passIssuer, NotificationService notificationService,
                                RoleAccessChecker roleAccessChecker, AccessCheckService accessCheckService) {
        this.accessListMapper = accessListMapper;
        this.userMapper = userMapper;
        this.userRoleMapper = userRoleMapper;
        this.recordMapper = recordMapper;
        this.passIssuer = passIssuer;
        this.notificationService = notificationService;
        this.roleAccessChecker = roleAccessChecker;
        this.accessCheckService = accessCheckService;
    }

    @Override
    public PageResult<AccessList> page(String keyword, int page, int size) {
        requireViewer();
        int safePage = Math.max(page, 1);
        int safeSize = Math.min(Math.max(size, 1), 100);
        LambdaQueryWrapper<AccessList> query = new LambdaQueryWrapper<AccessList>()
                .eq(AccessList::getSchoolId, SchoolScopes.DEFAULT_SCHOOL_ID)
                .eq(AccessList::getListType, LIST_WHITE)
                .eq(AccessList::getTargetType, "PERSON")
                .and(keyword != null && !keyword.isBlank(), wrapper -> wrapper
                        .like(AccessList::getPersonName, keyword)
                        .or().like(AccessList::getTargetValue, keyword)
                        .or().like(AccessList::getPlateNumber, keyword))
                .orderByDesc(AccessList::getId);
        Page<AccessList> result = accessListMapper.selectPage(new Page<>(safePage, safeSize), query);
        return PageResult.of(result.getTotal(), safePage, safeSize, result.getRecords());
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public AccessList create(WhitelistSaveDTO dto) {
        requireUploader();
        return createInternal(dto, "MANUAL");
    }

    private AccessList createInternal(WhitelistSaveDTO dto, String source) {
        String name = dto.getPersonName() == null ? "" : dto.getPersonName().trim();
        String phone = normalize(dto.getPhone());
        String plate = normalizePlate(dto.getPlateNumber());
        if (name.isEmpty()) {
            throw new BizException(400, "姓名不能为空");
        }
        if (phone.isEmpty()) {
            throw new BizException(400, "手机号不能为空");
        }
        if (dto.getEffectiveFrom() == null || dto.getEffectiveTo() == null) {
            throw new BizException(400, "生效时间和失效时间不能为空");
        }
        if (!dto.getEffectiveFrom().isBefore(dto.getEffectiveTo())) {
            throw new BizException(400, "失效时间必须晚于生效时间");
        }
        if (!dto.getEffectiveTo().isAfter(LocalDateTime.now())) {
            throw new BizException(400, "失效时间必须晚于当前时间");
        }
        assertNotBlacklisted(phone, plate);

        AccessList entry = accessListMapper.selectOne(new LambdaQueryWrapper<AccessList>()
                .eq(AccessList::getSchoolId, SchoolScopes.DEFAULT_SCHOOL_ID)
                .eq(AccessList::getListType, LIST_WHITE)
                .eq(AccessList::getTargetType, "PERSON")
                .eq(AccessList::getTargetValue, phone)
                .last("LIMIT 1"));
        boolean reuse = entry != null;
        if (reuse && (entry.getStatus() == PENDING || entry.getStatus() == ACTIVE)) {
            throw new BizException(400, "该手机号已在白名单中");
        }
        if (!reuse) {
            entry = new AccessList();
        }
        entry.setSchoolId(SchoolScopes.DEFAULT_SCHOOL_ID);
        entry.setListType(LIST_WHITE);
        entry.setTargetType("PERSON");
        entry.setTargetValue(phone);
        entry.setPersonName(name);
        entry.setPlateNumber(plate.isEmpty() ? null : plate);
        entry.setEffectiveFrom(dto.getEffectiveFrom());
        entry.setEffectiveTo(dto.getEffectiveTo());
        entry.setReason(dto.getReason());
        entry.setSource(source);
        entry.setCreatedBy(UserContext.getUserId());
        boolean securityAdmin = roleAccessChecker.hasAny(RoleEnum.SECURITY_ADMIN, RoleEnum.SYSTEM_ADMIN);
        entry.setStatus(securityAdmin ? ACTIVE : PENDING);
        entry.setApprovedBy(null);
        entry.setApprovedAt(null);
        entry.setRejectReason(null);
        entry.setVisitorRecordId(null);
        entry.setCreatedAt(reuse ? entry.getCreatedAt() : LocalDateTime.now());
        entry.setUpdatedAt(LocalDateTime.now());
        if (reuse) {
            accessListMapper.updateById(entry);
        } else {
            accessListMapper.insert(entry);
        }

        if (securityAdmin) {
            generateRecord(entry);
        } else {
            notifySecurityAdmins(entry);
        }
        return entry;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public AccessList approve(Long id) {
        requireApprover();
        AccessList entry = getWhitelist(id);
        if (entry.getStatus() != PENDING) {
            throw new BizException(400, "该白名单不是待审批状态");
        }
        entry.setStatus(ACTIVE);
        entry.setApprovedBy(UserContext.getUserId());
        entry.setApprovedAt(LocalDateTime.now());
        entry.setRejectReason(null);
        entry.setUpdatedAt(LocalDateTime.now());
        assertNotBlacklisted(entry.getTargetValue(), entry.getPlateNumber());
        accessListMapper.updateById(entry);
        generateRecord(entry);
        return entry;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public AccessList reject(Long id, String reason) {
        requireApprover();
        AccessList entry = getWhitelist(id);
        if (entry.getStatus() != PENDING) {
            throw new BizException(400, "该白名单不是待审批状态");
        }
        entry.setStatus(REJECTED);
        entry.setApprovedBy(UserContext.getUserId());
        entry.setApprovedAt(LocalDateTime.now());
        entry.setRejectReason(reason);
        entry.setUpdatedAt(LocalDateTime.now());
        accessListMapper.updateById(entry);
        return entry;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public AccessList updateStatus(Long id, Integer status) {
        requireApprover();
        if (status == null || (status != ACTIVE && status != DISABLED && status != REJECTED)) {
            throw new BizException(400, "不支持的白名单状态");
        }
        AccessList entry = getWhitelist(id);
        if (status == ACTIVE) {
            if (entry.getStatus() == ACTIVE) {
                return entry;
            }
            entry.setStatus(ACTIVE);
            entry.setApprovedBy(UserContext.getUserId());
            entry.setApprovedAt(LocalDateTime.now());
            entry.setUpdatedAt(LocalDateTime.now());
            assertNotBlacklisted(entry.getTargetValue(), entry.getPlateNumber());
            accessListMapper.updateById(entry);
            generateRecord(entry);
        } else {
            entry.setStatus(status);
            entry.setUpdatedAt(LocalDateTime.now());
            accessListMapper.updateById(entry);
            revokeRecord(entry);
        }
        return entry;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void delete(Long id) {
        requireApprover();
        AccessList entry = getWhitelist(id);
        revokeRecord(entry);
        accessListMapper.deleteById(id);
    }

    @Override
    public Map<String, Object> importExcel(MultipartFile file) {
        requireUploader();
        try {
            List<WhitelistImportRow> rows = EasyExcel.read(file.getInputStream())
                    .head(WhitelistImportRow.class).sheet().doReadSync();
            return importRows(rows);
        } catch (IOException e) {
            throw new BizException(400, "Excel 读取失败");
        }
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Map<String, Object> importRows(List<WhitelistImportRow> rows) {
        requireUploader();
        int success = 0;
        List<Map<String, Object>> errors = new ArrayList<>();
        for (int i = 0; i < rows.size(); i++) {
            WhitelistImportRow row = rows.get(i);
            try {
                WhitelistSaveDTO dto = new WhitelistSaveDTO();
                dto.setPersonName(row.getPersonName());
                dto.setPhone(row.getPhone());
                dto.setPlateNumber(row.getPlateNumber());
                dto.setEffectiveFrom(parseTime(row.getEffectiveFrom()));
                dto.setEffectiveTo(parseTime(row.getEffectiveTo()));
                dto.setReason(row.getReason());
                createInternal(dto, "EXCEL");
                success++;
            } catch (Exception e) {
                Map<String, Object> error = new LinkedHashMap<>();
                error.put("row", i + 2);
                error.put("reason", e.getMessage() == null ? "导入失败" : e.getMessage());
                errors.add(error);
            }
        }
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("success", success);
        result.put("failed", errors.size());
        result.put("errors", errors);
        return result;
    }

    private void generateRecord(AccessList entry) {
        if (entry.getVisitorRecordId() != null) {
            VisitorRecord existing = recordMapper.selectById(entry.getVisitorRecordId());
            if (existing != null && existing.getStatus() != 5) {
                return;
            }
        }
        User person = userMapper.selectOne(new LambdaQueryWrapper<User>()
                .eq(User::getPhone, entry.getTargetValue())
                .last("LIMIT 1"));
        VisitorRecord record = new VisitorRecord();
        record.setSchoolId(entry.getSchoolId());
        record.setVisitorUserId(person != null ? person.getId() : UserContext.getUserId());
        record.setVisitorName(entry.getPersonName());
        record.setVisitorPhone(entry.getTargetValue());
        record.setVisitorType("NORMAL");
        record.setPlateNumber(entry.getPlateNumber());
        record.setPassMode(entry.getPlateNumber() == null || entry.getPlateNumber().isBlank() ? "WALK" : "CAR");
        record.setTargetType("WHITELIST");
        record.setTargetName("白名单");
        record.setVisitPurpose(entry.getReason() == null ? "白名单通行" : entry.getReason());
        record.setExpectedTime(entry.getEffectiveFrom());
        record.setLeaveTime(entry.getEffectiveTo());
        record.setStatus(ACTIVE);
        record.setCreatedAt(LocalDateTime.now());
        record.setUpdatedAt(LocalDateTime.now());
        recordMapper.insert(record);
        passIssuer.issue(record);
        entry.setVisitorRecordId(record.getId());
        entry.setUpdatedAt(LocalDateTime.now());
        accessListMapper.updateById(entry);
    }

    private void revokeRecord(AccessList entry) {
        if (entry.getVisitorRecordId() == null) {
            return;
        }
        VisitorRecord record = recordMapper.selectById(entry.getVisitorRecordId());
        if (record == null) {
            return;
        }
        passIssuer.revoke(record.getId());
        record.setStatus(5);
        record.setUpdatedAt(LocalDateTime.now());
        recordMapper.updateById(record);
        entry.setVisitorRecordId(null);
        accessListMapper.updateById(entry);
    }

    private void notifySecurityAdmins(AccessList entry) {
        Set<Long> userIds = new LinkedHashSet<>();
        for (UserRole role : userRoleMapper.selectList(new LambdaQueryWrapper<UserRole>()
                .eq(UserRole::getRole, RoleEnum.SECURITY_ADMIN)
                .eq(UserRole::getStatus, 1))) {
            userIds.add(role.getUserId());
        }
        for (Long userId : userIds) {
            User user = userMapper.selectById(userId);
            if (user == null || !Integer.valueOf(1).equals(user.getStatus())) {
                continue;
            }
            notificationService.notifyUser(userId, "新的白名单待审批",
                    entry.getPersonName() + "（" + entry.getTargetValue() + "）提交了白名单申请",
                    "APPROVAL", "WHITELIST", entry.getId());
        }
    }

    private void assertNotBlacklisted(String phone, String plate) {
        String access = accessCheckService.check(SchoolScopes.DEFAULT_SCHOOL_ID, phone, plate);
        if ("BLACKLIST".equals(access)) {
            throw new BizException(400, "该人员或车牌在黑名单中，不能加入白名单");
        }
    }
    private AccessList getWhitelist(Long id) {        AccessList entry = accessListMapper.selectById(id);
        if (entry == null || entry.getListType() != LIST_WHITE || !"PERSON".equals(entry.getTargetType())) {
            throw new BizException(404, "白名单不存在");
        }
        if (!SchoolScopes.DEFAULT_SCHOOL_ID.equals(entry.getSchoolId())) {
            throw new BizException(403, "无权操作该白名单");
        }
        return entry;
    }

    private void requireViewer() {
        if (!roleAccessChecker.hasAny(RoleEnum.SECURITY_ADMIN, RoleEnum.SYSTEM_ADMIN, RoleEnum.DEPT_OPERATOR)) {
            throw new BizException(403, "无权查看白名单");
        }
    }

    private void requireUploader() {
        requireViewer();
    }

    private void requireApprover() {
        if (!roleAccessChecker.hasAny(RoleEnum.SECURITY_ADMIN, RoleEnum.SYSTEM_ADMIN)) {
            throw new BizException(403, "仅保卫处管理员可审批白名单");
        }
    }

    private String normalize(String value) {
        return value == null ? "" : value.trim();
    }

    private String normalizePlate(String value) {
        return normalize(value).replace(" ", "").toUpperCase();
    }

    private LocalDateTime parseTime(String value) {
        String text = normalize(value);
        if (text.isEmpty()) {
            return null;
        }
        try {
            return LocalDateTime.parse(text.replace("T", " "), FORMAT);
        } catch (Exception ignored) {
            return LocalDate.parse(text).atStartOfDay();
        }
    }
}
