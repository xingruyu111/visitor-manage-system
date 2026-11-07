package com.visitor.alarm;

import com.visitor.common.enums.RoleEnum;
import com.visitor.domain.entity.User;
import com.visitor.domain.entity.UserRole;
import com.visitor.domain.entity.VisitorRecord;
import com.visitor.server.mapper.AlarmRecordMapper;
import com.visitor.server.mapper.NotificationMapper;
import com.visitor.server.mapper.UserMapper;
import com.visitor.server.mapper.UserRoleMapper;
import com.visitor.server.mapper.VisitorRecordMapper;
import com.visitor.server.service.AlarmService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@ActiveProfiles("test")
class AlarmServiceTest {

    @Autowired
    private AlarmService service;

    @Autowired
    private AlarmRecordMapper alarmMapper;

    @Autowired
    private VisitorRecordMapper recordMapper;

    @Autowired
    private NotificationMapper notificationMapper;

    @Autowired
    private UserMapper userMapper;

    @Autowired
    private UserRoleMapper userRoleMapper;

    @BeforeEach
    void setUp() {
        alarmMapper.delete(null);
        notificationMapper.delete(null);
        recordMapper.delete(null);
        userRoleMapper.delete(null);
        userMapper.delete(null);
    }

    @Test
    void overstayCreatesAlarmAndNotificationOnce() {
        Long securityId = createSecurity();
        Long recordId = createInSchoolRecord(LocalDateTime.now().minusHours(13));

        assertThat(service.checkOverstay()).isEqualTo(1);
        assertThat(alarmMapper.selectCount(null)).isEqualTo(1L);
        assertThat(notificationMapper.selectCount(null)).isEqualTo(1L);
        assertThat(notificationMapper.selectList(null).get(0).getUserId()).isEqualTo(securityId);

        assertThat(service.checkOverstay()).isZero();
        assertThat(service.page(0, 1, 10).total()).isEqualTo(1);
    }

    @Test
    void notOverdueIsIgnored() {
        createSecurity();
        createInSchoolRecord(LocalDateTime.now().minusHours(1));

        assertThat(service.checkOverstay()).isZero();
    }

    @Test
    void alarmCanBeHandled() {
        createSecurity();
        createInSchoolRecord(LocalDateTime.now().minusHours(13));
        service.checkOverstay();
        Long alarmId = alarmMapper.selectList(null).get(0).getId();

        service.handle(alarmId, 1L);

        assertThat(alarmMapper.selectById(alarmId).getHandled()).isEqualTo(1);
        assertThat(service.page(0, 1, 10).total()).isZero();
        assertThat(service.page(1, 1, 10).total()).isEqualTo(1);
    }

    private Long createSecurity() {
        User user = new User();
        user.setPhone("13800007777");
        user.setName("保卫处");
        user.setPassword("x");
        user.setStatus(1);
        user.setCreatedAt(LocalDateTime.now());
        user.setUpdatedAt(LocalDateTime.now());
        userMapper.insert(user);

        UserRole role = new UserRole();
        role.setUserId(user.getId());
        role.setRole(RoleEnum.SECURITY_ADMIN);
        role.setSchoolId(1L);
        role.setIsDefault(1);
        role.setStatus(1);
        role.setCreatedAt(LocalDateTime.now());
        role.setUpdatedAt(LocalDateTime.now());
        userRoleMapper.insert(role);
        return user.getId();
    }

    private Long createInSchoolRecord(LocalDateTime leaveTime) {
        VisitorRecord record = new VisitorRecord();
        record.setSchoolId(1L);
        record.setVisitorUserId(1L);
        record.setVisitorName("滞留访客");
        record.setVisitorType("NORMAL");
        record.setExpectedTime(leaveTime.minusHours(2));
        record.setLeaveTime(leaveTime);
        record.setStatus(3);
        record.setCreatedAt(LocalDateTime.now());
        record.setUpdatedAt(LocalDateTime.now());
        recordMapper.insert(record);
        return record.getId();
    }
}
