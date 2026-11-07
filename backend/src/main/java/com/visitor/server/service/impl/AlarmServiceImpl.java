package com.visitor.server.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.visitor.common.core.PageResult;
import com.visitor.common.enums.RoleEnum;
import com.visitor.common.exception.BizException;
import com.visitor.domain.entity.AlarmRecord;
import com.visitor.domain.entity.UserRole;
import com.visitor.domain.entity.VisitorRecord;
import com.visitor.server.mapper.AlarmRecordMapper;
import com.visitor.server.mapper.UserRoleMapper;
import com.visitor.server.mapper.VisitorRecordMapper;
import com.visitor.server.service.AlarmService;
import com.visitor.server.service.NotificationService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Service
public class AlarmServiceImpl implements AlarmService {

    private static final Logger log = LoggerFactory.getLogger(AlarmServiceImpl.class);

    private final AlarmRecordMapper alarmMapper;
    private final VisitorRecordMapper recordMapper;
    private final UserRoleMapper userRoleMapper;
    private final NotificationService notificationService;

    @Value("${visitor.alarm.max-stay-hours:12}")
    private int maxStayHours;

    public AlarmServiceImpl(AlarmRecordMapper alarmMapper, VisitorRecordMapper recordMapper,
                            UserRoleMapper userRoleMapper, NotificationService notificationService) {
        this.alarmMapper = alarmMapper;
        this.recordMapper = recordMapper;
        this.userRoleMapper = userRoleMapper;
        this.notificationService = notificationService;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public int checkOverstay() {
        LocalDateTime deadline = LocalDateTime.now().minusHours(maxStayHours);
        List<VisitorRecord> overstays = recordMapper.selectList(new LambdaQueryWrapper<VisitorRecord>()
                .eq(VisitorRecord::getStatus, 3)
                .lt(VisitorRecord::getLeaveTime, deadline));
        int created = 0;
        for (VisitorRecord record : overstays) {
            Long exists = alarmMapper.selectCount(new LambdaQueryWrapper<AlarmRecord>()
                    .eq(AlarmRecord::getRecordId, record.getId())
                    .eq(AlarmRecord::getAlarmType, "OVERSTAY"));
            if (exists != null && exists > 0) {
                continue;
            }
            AlarmRecord alarm = new AlarmRecord();
            alarm.setRecordId(record.getId());
            alarm.setAlarmType("OVERSTAY");
            alarm.setMessage(record.getVisitorName() + " 超过预计离校时间仍未离校");
            alarm.setHandled(0);
            alarm.setCreatedAt(LocalDateTime.now());
            alarmMapper.insert(alarm);
            notifySecurity(record);
            created++;
            log.info("超期滞留告警: recordId={}", record.getId());
        }
        return created;
    }

    @Override
    public PageResult<AlarmRecord> page(Integer handled, int page, int size) {
        int safePage = Math.max(page, 1);
        int safeSize = Math.min(Math.max(size, 1), 100);
        Page<AlarmRecord> result = alarmMapper.selectPage(new Page<>(safePage, safeSize),
                new LambdaQueryWrapper<AlarmRecord>()
                        .eq(handled != null, AlarmRecord::getHandled, handled)
                        .orderByDesc(AlarmRecord::getId));
        return PageResult.of(result.getTotal(), safePage, safeSize, result.getRecords());
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void handle(Long id, Long operatorId) {
        AlarmRecord alarm = alarmMapper.selectById(id);
        if (alarm == null) {
            throw new BizException(404, "告警不存在");
        }
        alarm.setHandled(1);
        alarm.setHandledBy(operatorId);
        alarm.setHandledAt(LocalDateTime.now());
        alarmMapper.updateById(alarm);
    }

    @Override
    public void notifyBlacklistHit(String visitorName, String phone) {
        List<UserRole> roles = userRoleMapper.selectList(new LambdaQueryWrapper<UserRole>()
                .eq(UserRole::getRole, RoleEnum.SECURITY_ADMIN)
                .eq(UserRole::getStatus, 1));
        for (UserRole role : roles) {
            notificationService.notifyUser(role.getUserId(), "黑名单拦截告警",
                    (visitorName == null ? "访客" : visitorName) + "（" + (phone == null ? "-" : phone)
                            + "）命中黑名单，已拦截",
                    "BLACKLIST_ALERT", "ACCESS_LIST", null);
        }
    }

    private void notifySecurity(VisitorRecord record) {
        List<UserRole> roles = userRoleMapper.selectList(new LambdaQueryWrapper<UserRole>()
                .eq(UserRole::getRole, RoleEnum.SECURITY_ADMIN)
                .eq(UserRole::getStatus, 1));
        for (UserRole role : roles) {
            notificationService.notifyUser(role.getUserId(), "超期滞留告警",
                    record.getVisitorName() + " 仍未离校，请及时处理",
                    "ACCESS_ALERT", "VISITOR_RECORD", record.getId());
        }
    }
}
