package com.visitor.org;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.visitor.common.enums.PersonTypeEnum;
import com.visitor.common.enums.RoleEnum;
import com.visitor.domain.dto.StaffSaveDTO;
import com.visitor.domain.entity.Staff;
import com.visitor.domain.entity.User;
import com.visitor.domain.entity.UserRole;
import com.visitor.domain.vo.StaffVO;
import com.visitor.server.mapper.StaffMapper;
import com.visitor.server.mapper.UserMapper;
import com.visitor.server.mapper.UserRoleMapper;
import com.visitor.server.service.StaffService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@ActiveProfiles("test")
class StaffAccountRestoreTest {

    @Autowired
    private StaffService staffService;

    @Autowired
    private StaffMapper staffMapper;

    @Autowired
    private UserMapper userMapper;

    @Autowired
    private UserRoleMapper userRoleMapper;

    @BeforeEach
    void setUp() {
        staffMapper.delete(null);
        userRoleMapper.delete(null);
        userMapper.delete(null);
    }

    @Test
    void reAddingStaffRestoresDisabledAccount() {
        User user = new User();
        user.setPhone("13900009999");
        user.setName("历史老师");
        user.setPassword("x");
        user.setStatus(0);
        user.setCreatedAt(LocalDateTime.now());
        user.setUpdatedAt(LocalDateTime.now());
        userMapper.insert(user);
        UserRole role = new UserRole();
        role.setUserId(user.getId());
        role.setRole(RoleEnum.TEACHER);
        role.setSchoolId(1L);
        role.setIsDefault(1);
        role.setStatus(0);
        role.setCreatedAt(LocalDateTime.now());
        role.setUpdatedAt(LocalDateTime.now());
        userRoleMapper.insert(role);

        StaffSaveDTO dto = new StaffSaveDTO();
        dto.setName("历史老师");
        dto.setPhone("13900009999");
        dto.setPersonType(PersonTypeEnum.TEACHER.getCode());
        StaffVO created = staffService.create(1L, dto);

        assertThat(userMapper.selectById(user.getId()).getStatus()).isEqualTo(1);
        assertThat(userRoleMapper.selectList(new LambdaQueryWrapper<UserRole>()
                .eq(UserRole::getUserId, user.getId())).get(0).getStatus()).isEqualTo(1);

        staffService.deactivate(1L, created.getId());
        assertThat(userRoleMapper.selectList(new LambdaQueryWrapper<UserRole>()
                .eq(UserRole::getUserId, user.getId())).get(0).getStatus()).isZero();

        StaffVO restored = staffService.create(1L, dto);
        assertThat(restored.getId()).isEqualTo(created.getId());
        assertThat(userMapper.selectById(user.getId()).getStatus()).isEqualTo(1);
        Staff staff = staffMapper.selectById(created.getId());
        assertThat(staff.getStatus()).isEqualTo(1);
    }
}
