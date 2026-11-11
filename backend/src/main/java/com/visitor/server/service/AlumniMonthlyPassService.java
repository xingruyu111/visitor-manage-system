package com.visitor.server.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.visitor.common.enums.PersonTypeEnum;
import com.visitor.common.enums.RoleEnum;
import com.visitor.domain.entity.Staff;
import com.visitor.domain.entity.User;
import com.visitor.domain.entity.VisitorRecord;
import com.visitor.security.RoleAccessChecker;
import com.visitor.security.UserContext;
import com.visitor.server.mapper.StaffMapper;
import com.visitor.server.mapper.UserMapper;
import com.visitor.server.mapper.VisitorRecordMapper;
import com.visitor.server.service.impl.VisitorPassIssuer;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.YearMonth;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Service
public class AlumniMonthlyPassService {

    private final VisitConfigService visitConfigService;
    private final RoleAccessChecker roleAccessChecker;
    private final UserMapper userMapper;
    private final StaffMapper staffMapper;
    private final VisitorRecordMapper recordMapper;
    private final VisitorPassIssuer passIssuer;

    public AlumniMonthlyPassService(VisitConfigService visitConfigService, RoleAccessChecker roleAccessChecker,
                                    UserMapper userMapper, StaffMapper staffMapper,
                                    VisitorRecordMapper recordMapper, VisitorPassIssuer passIssuer) {
        this.visitConfigService = visitConfigService;
        this.roleAccessChecker = roleAccessChecker;
        this.userMapper = userMapper;
        this.staffMapper = staffMapper;
        this.recordMapper = recordMapper;
        this.passIssuer = passIssuer;
    }

    @Transactional(rollbackFor = Exception.class)
    public Map<String, Object> currentMonth() {
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("eligible", false);
        if (!isAlumni()) {
            return result;
        }
        YearMonth month = YearMonth.now();
        List<Integer> months = visitConfigService.alumniMonths();
        if (!months.contains(month.getMonthValue())) {
            return result;
        }
        Long userId = UserContext.getUserId();
        User user = userMapper.selectById(userId);
        if (user == null) {
            return result;
        }
        LocalDateTime from = month.atDay(1).atStartOfDay();
        LocalDateTime to = month.atEndOfMonth().atTime(23, 59, 59);
        VisitorRecord record = recordMapper.selectOne(new LambdaQueryWrapper<VisitorRecord>()
                .eq(VisitorRecord::getVisitorUserId, userId)
                .eq(VisitorRecord::getTargetType, "ALUMNI_MONTH")
                .ge(VisitorRecord::getExpectedTime, from)
                .le(VisitorRecord::getExpectedTime, to)
                .last("LIMIT 1"));
        if (record == null) {
            record = new VisitorRecord();
            record.setSchoolId(1L);
            record.setVisitorUserId(userId);
            record.setVisitorName(user.getName() == null ? "校友" : user.getName());
            record.setVisitorPhone(user.getPhone());
            record.setVisitorType("ALUMNI");
            record.setTargetType("ALUMNI_MONTH");
            record.setTargetName("校友自然月通行");
            record.setVisitPurpose("校友自然月通行");
            record.setExpectedTime(from);
            record.setLeaveTime(to);
            record.setStatus(1);
            record.setCreatedAt(LocalDateTime.now());
            record.setUpdatedAt(LocalDateTime.now());
            recordMapper.insert(record);
            passIssuer.issue(record);
        }
        result.put("eligible", true);
        result.put("month", month.getMonthValue());
        result.put("recordId", record.getId());
        result.put("validFrom", from);
        result.put("validTo", to);
        return result;
    }

    private boolean isAlumni() {
        if (roleAccessChecker.hasAny(RoleEnum.ALUMNI)) {
            return true;
        }
        User user = userMapper.selectById(UserContext.getUserId());
        if (user == null || user.getPhone() == null) {
            return false;
        }
        Staff staff = staffMapper.selectOne(new LambdaQueryWrapper<Staff>()
                .eq(Staff::getPhone, user.getPhone())
                .eq(Staff::getStatus, 1)
                .last("LIMIT 1"));
        return staff != null && staff.getPersonType() == PersonTypeEnum.ALUMNI;
    }
}
