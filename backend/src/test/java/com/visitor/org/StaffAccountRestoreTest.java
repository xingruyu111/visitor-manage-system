package com.visitor.org;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.visitor.common.enums.PersonTypeEnum;
import com.visitor.common.enums.RoleEnum;
import com.visitor.common.exception.BizException;
import com.visitor.domain.dto.StaffSaveDTO;
import com.visitor.domain.entity.ApprovalTask;
import com.visitor.domain.entity.Org;
import com.visitor.domain.entity.Staff;
import com.visitor.domain.entity.User;
import com.visitor.domain.entity.UserRole;
import com.visitor.domain.vo.StaffVO;
import com.visitor.server.mapper.ApprovalTaskMapper;
import com.visitor.server.mapper.OrgMapper;
import com.visitor.server.mapper.StaffMapper;
import com.visitor.server.mapper.UserMapper;
import com.visitor.server.mapper.UserRoleMapper;
import com.visitor.server.service.StaffService;
import com.visitor.server.service.UserAdminService;
import com.visitor.workflow.ApprovalStatuses;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@SpringBootTest
@ActiveProfiles("test")
class StaffAccountRestoreTest {

    @Autowired
    private StaffService staffService;

    @Autowired
    private UserAdminService userAdminService;

    @Autowired
    private StaffMapper staffMapper;

    @Autowired
    private UserMapper userMapper;

    @Autowired
    private UserRoleMapper userRoleMapper;

    @Autowired
    private ApprovalTaskMapper taskMapper;

    @Autowired
    private OrgMapper orgMapper;

    @BeforeEach
    void setUp() {
        taskMapper.delete(null);
        orgMapper.delete(null);
        staffMapper.delete(null);
        userRoleMapper.delete(null);
        userMapper.delete(null);
    }

    @Test
    void createStaffCreatesAccountWithPrimaryRole() {
        StaffVO created = staffService.create(1L, dto("T100", "13900009999", PersonTypeEnum.TEACHER));

        User user = userMapper.selectOne(new LambdaQueryWrapper<User>()
                .eq(User::getPersonNo, "T100"));
        assertThat(user).isNotNull();
        assertThat(user.getStatus()).isEqualTo(1);
        assertThat(user.getPhone()).isEqualTo("13900009999");

        List<UserRole> roles = roles(user.getId());
        assertThat(roles).hasSize(1);
        assertThat(roles.get(0).getRole()).isEqualTo(RoleEnum.TEACHER);
        assertThat(roles.get(0).getSource()).isEqualTo("IDENTITY");
        assertThat(roles.get(0).getIsDefault()).isEqualTo(1);

        assertThat(created.getPersonNo()).isEqualTo("T100");
    }

    @Test
    void deactivateBlocksOnPendingTasksThenRestoresSameAccount() {
        StaffVO created = staffService.create(1L, dto("T101", "13900008888", PersonTypeEnum.TEACHER));
        User user = userMapper.selectOne(new LambdaQueryWrapper<User>().eq(User::getPersonNo, "T101"));

        ApprovalTask task = new ApprovalTask();
        task.setInstanceId(1L);
        task.setSeq(1);
        task.setNodeCode("N1");
        task.setAssigneeType("TARGET");
        task.setAssigneeId(user.getId());
        task.setStatus(ApprovalStatuses.PENDING);
        task.setCreatedAt(LocalDateTime.now());
        taskMapper.insert(task);

        assertThatThrownBy(() -> staffService.deactivate(1L, created.getId()))
                .isInstanceOf(BizException.class)
                .hasMessageContaining("待审批");

        taskMapper.deleteById(task.getId());
        user.setPhone("13900007777");
        userMapper.updateById(user);

        staffService.deactivate(1L, created.getId());
        assertThat(staffMapper.selectById(created.getId()).getStatus()).isZero();
        assertThat(userMapper.selectById(user.getId()).getStatus()).isZero();
        assertThat(roles(user.getId())).allMatch(role -> role.getStatus() == 1);

        StaffVO restored = staffService.create(1L, dto("T101", "13900006666", PersonTypeEnum.TEACHER));
        assertThat(restored.getId()).isEqualTo(created.getId());
        User restoredUser = userMapper.selectById(user.getId());
        assertThat(restoredUser.getStatus()).isEqualTo(1);
        assertThat(restoredUser.getPhone()).isEqualTo("13900006666");
        assertThat(roles(user.getId())).allMatch(role -> role.getStatus() == 1);
    }

    @Test
    void typeChangeReplacesPrimaryRoleButKeepsExtraRole() {
        StaffVO created = staffService.create(1L, dto("T102", "13900005555", PersonTypeEnum.STUDENT));
        User user = userMapper.selectOne(new LambdaQueryWrapper<User>().eq(User::getPersonNo, "T102"));
        userAdminService.addRole(user.getId(), RoleEnum.GUARD.getCode());

        staffService.update(1L, created.getId(), dto("T102", "13900005555", PersonTypeEnum.TEACHER));

        List<UserRole> roles = roles(user.getId());
        assertThat(roles).extracting(UserRole::getRole)
                .containsExactlyInAnyOrder(RoleEnum.TEACHER, RoleEnum.GUARD);
        UserRole primary = roles.stream().filter(role -> "IDENTITY".equals(role.getSource())).findFirst().orElseThrow();
        assertThat(primary.getRole()).isEqualTo(RoleEnum.TEACHER);
        assertThat(primary.getIsDefault()).isEqualTo(1);
    }

    @Test
    void phoneUpdateSyncsAccountAndAccountSideIsBlocked() {
        staffService.create(1L, dto("T103", "13900004444", PersonTypeEnum.TEACHER));
        User user = userMapper.selectOne(new LambdaQueryWrapper<User>().eq(User::getPersonNo, "T103"));

        Staff staff = staffMapper.selectOne(new LambdaQueryWrapper<Staff>().eq(Staff::getPersonNo, "T103"));
        staffService.update(1L, staff.getId(), dto("T103", "13900003333", PersonTypeEnum.TEACHER));

        assertThat(userMapper.selectById(user.getId()).getPhone()).isEqualTo("13900003333");
        assertThatThrownBy(() -> userAdminService.updatePhone(user.getId(), "13900002222"))
                .isInstanceOf(BizException.class)
                .hasMessageContaining("人员管理");
    }

    @Test
    void identityRoleCannotBeRemoved() {
        staffService.create(1L, dto("T104", "13900001111", PersonTypeEnum.TEACHER));
        User user = userMapper.selectOne(new LambdaQueryWrapper<User>().eq(User::getPersonNo, "T104"));

        assertThatThrownBy(() -> userAdminService.removeRole(user.getId(), RoleEnum.TEACHER.getCode()))
                .isInstanceOf(BizException.class)
                .hasMessageContaining("人员类型决定");
    }
    @Test
    void enableRestoresAccountAfterDeactivate() {
        StaffVO created = staffService.create(1L, dto("T105", "13900002222", PersonTypeEnum.TEACHER));
        User user = userMapper.selectOne(new LambdaQueryWrapper<User>().eq(User::getPersonNo, "T105"));

        staffService.deactivate(1L, created.getId());
        assertThat(userMapper.selectById(user.getId()).getStatus()).isZero();

        staffService.updateStatus(1L, created.getId(), 1);
        assertThat(staffMapper.selectById(created.getId()).getStatus()).isEqualTo(1);
        assertThat(userMapper.selectById(user.getId()).getStatus()).isEqualTo(1);
        assertThat(roles(user.getId())).allMatch(role -> role.getStatus() == 1);
    }

    @Test
    void softDeleteHidesAndReAddRestores() {
        StaffVO created = staffService.create(1L, dto("T106", "13900003344", PersonTypeEnum.TEACHER));
        User user = userMapper.selectOne(new LambdaQueryWrapper<User>().eq(User::getPersonNo, "T106"));

        staffService.delete(1L, created.getId());
        assertThat(staffMapper.selectById(created.getId()).getIsDeleted()).isEqualTo(1);
        assertThat(userMapper.selectById(user.getId()).getStatus()).isZero();
        assertThat(staffService.page(1L, null, null, null, null, 1, 10).total()).isZero();

        StaffVO restored = staffService.create(1L, dto("T106", "13900003344", PersonTypeEnum.TEACHER));
        assertThat(restored.getId()).isEqualTo(created.getId());
        assertThat(staffMapper.selectById(created.getId()).getIsDeleted()).isZero();
        assertThat(staffMapper.selectById(created.getId()).getStatus()).isEqualTo(1);
        assertThat(userMapper.selectById(user.getId()).getStatus()).isEqualTo(1);
        assertThat(staffService.page(1L, null, null, null, null, 1, 10).total()).isEqualTo(1);
    }
    @Test
    void phoneCanBeReusedAfterDelete() {
        StaffVO first = staffService.create(1L, dto("T107", "13900004455", PersonTypeEnum.TEACHER));
        User user = userMapper.selectOne(new LambdaQueryWrapper<User>().eq(User::getPersonNo, "T107"));
        staffService.delete(1L, first.getId());

        StaffVO second = staffService.create(1L, dto("T108", "13900004455", PersonTypeEnum.STUDENT));

        assertThat(second.getId()).isNotEqualTo(first.getId());
        User rebound = userMapper.selectById(user.getId());
        assertThat(rebound.getPersonNo()).isEqualTo("T108");
        assertThat(rebound.getStatus()).isEqualTo(1);
        assertThat(roles(user.getId())).anyMatch(role -> role.getRole() == RoleEnum.STUDENT
                && "IDENTITY".equals(role.getSource()));
    }
    @Test
    void clearingPositionAndOrgIsPersisted() {
        Org org = new Org();
        org.setName("清空测试学院");
        org.setLevel(1);
        org.setStatus(1);
        org.setCreatedAt(LocalDateTime.now());
        org.setUpdatedAt(LocalDateTime.now());
        orgMapper.insert(org);

        StaffSaveDTO create = dto("T109", "13900006677", PersonTypeEnum.TEACHER);
        create.setPosition("老师");
        create.setOrgId(org.getId());
        StaffVO created = staffService.create(1L, create);
        assertThat(created.getOrgName()).isEqualTo("清空测试学院");

        StaffSaveDTO update = dto("T109", "13900006677", PersonTypeEnum.TEACHER);
        staffService.update(1L, created.getId(), update);

        Staff saved = staffMapper.selectById(created.getId());
        assertThat(saved.getPosition()).isNull();
        assertThat(saved.getOrgId()).isNull();
    }
    @Test
    void phoneChangeSyncsLinkedAccount() {
        StaffVO created = staffService.create(1L, dto("T110", "13900008801", PersonTypeEnum.TEACHER));
        User user = userMapper.selectOne(new LambdaQueryWrapper<User>().eq(User::getPersonNo, "T110"));

        StaffSaveDTO update = dto("T110", "13900008802", PersonTypeEnum.TEACHER);
        staffService.update(1L, created.getId(), update);

        assertThat(userMapper.selectById(user.getId()).getPhone()).isEqualTo("13900008802");
    }
    private StaffSaveDTO dto(String personNo, String phone, PersonTypeEnum type) {
        StaffSaveDTO dto = new StaffSaveDTO();
        dto.setPersonNo(personNo);
        dto.setPhone(phone);
        dto.setName("测试人员" + personNo);
        dto.setPersonType(type.getCode());
        return dto;
    }

    private List<UserRole> roles(Long userId) {
        return userRoleMapper.selectList(new LambdaQueryWrapper<UserRole>()
                .eq(UserRole::getUserId, userId));
    }
}
