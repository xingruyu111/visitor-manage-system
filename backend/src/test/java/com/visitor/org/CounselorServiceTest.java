package com.visitor.org;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.visitor.common.enums.PersonTypeEnum;
import com.visitor.common.exception.BizException;
import com.visitor.domain.dto.CounselorOrgSaveDTO;
import com.visitor.domain.entity.CounselorOrg;
import com.visitor.domain.entity.Org;
import com.visitor.domain.entity.Staff;
import com.visitor.domain.entity.User;
import com.visitor.domain.entity.UserRole;
import com.visitor.domain.vo.CounselorOrgVO;
import com.visitor.server.mapper.CounselorOrgMapper;
import com.visitor.server.mapper.OrgMapper;
import com.visitor.server.mapper.StaffMapper;
import com.visitor.server.mapper.UserMapper;
import com.visitor.server.mapper.UserRoleMapper;
import com.visitor.server.service.CounselorService;
import com.visitor.workflow.AssigneeResolver;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@SpringBootTest
@ActiveProfiles("test")
class CounselorServiceTest {

    @Autowired
    private CounselorService service;
    @Autowired
    private CounselorOrgMapper counselorOrgMapper;
    @Autowired
    private StaffMapper staffMapper;
    @Autowired
    private OrgMapper orgMapper;
    @Autowired
    private UserMapper userMapper;
    @Autowired
    private UserRoleMapper userRoleMapper;
    @Autowired
    private AssigneeResolver assigneeResolver;

    private Long rootId;
    private Long childId;
    private Long counselorId;
    private Long counselor2Id;
    private Long studentId;

    @BeforeEach
    void setUp() {
        counselorOrgMapper.delete(null);
        userRoleMapper.delete(null);
        userMapper.delete(null);
        staffMapper.delete(null);
        orgMapper.delete(null);

        rootId = org("人工智能学院", null, null, 1);
        childId = org("24软件1", rootId, String.valueOf(rootId), 2);
        counselorId = staff("张老师", "T3001", "13900003001", PersonTypeEnum.TEACHER, rootId);
        counselor2Id = staff("李老师", "T3002", "13900003002", PersonTypeEnum.TEACHER, childId);
        studentId = staff("学生甲", "S3001", "13900003003", PersonTypeEnum.STUDENT, childId);
    }

    @Test
    void createMappingAndResolveCounselor() {
        CounselorOrgVO vo = service.create(dto(counselorId, rootId));

        assertThat(vo.getOrgName()).isEqualTo("人工智能学院");
        assertThat(vo.getStudentCount()).isEqualTo(1);
        assertThat(service.candidates("张老师")).extracting("id").containsExactly(counselorId);
        assertThat(assigneeResolver.resolveUserId("COUNSELOR", studentId))
                .isEqualTo(userIdOf("13900003001"));
        assertThat(userRoleMapper.selectCount(new LambdaQueryWrapper<UserRole>()
                .eq(UserRole::getUserId, userIdOf("13900003001"))
                .eq(UserRole::getRole, com.visitor.common.enums.RoleEnum.COUNSELOR)))
                .isEqualTo(1);
    }

    @Test
    void nestedOrgOverridesAndFallsBackAfterDelete() {
        service.create(dto(counselorId, rootId));
        CounselorOrgVO childMapping = service.create(dto(counselor2Id, childId));

        assertThat(assigneeResolver.resolveUserId("COUNSELOR", studentId))
                .isEqualTo(userIdOf("13900003002"));

        service.delete(childMapping.getId());
        assertThat(assigneeResolver.resolveUserId("COUNSELOR", studentId))
                .isEqualTo(userIdOf("13900003001"));
    }

    @Test
    void duplicateOrgAndInvalidPersonAreRejected() {
        service.create(dto(counselorId, rootId));
        assertThatThrownBy(() -> service.create(dto(counselor2Id, rootId)))
                .isInstanceOf(BizException.class)
                .hasMessageContaining("该组织已配置辅导员");

        Long merchantId = staff("商户甲", "M3001", "13900003004", PersonTypeEnum.MERCHANT, rootId);
        assertThatThrownBy(() -> service.create(dto(merchantId, childId)))
                .isInstanceOf(BizException.class)
                .hasMessageContaining("只能选择教职工");
    }

    @Test
    void deleteLastMappingRemovesAutoRole() {
        CounselorOrgVO vo = service.create(dto(counselorId, rootId));
        Long userId = userIdOf("13900003001");

        service.delete(vo.getId());

        assertThat(counselorOrgMapper.selectCount(null)).isZero();
        assertThat(userRoleMapper.selectCount(new LambdaQueryWrapper<UserRole>()
                .eq(UserRole::getUserId, userId)
                .eq(UserRole::getRole, com.visitor.common.enums.RoleEnum.COUNSELOR))).isZero();
    }

    @Test
    void disabledCounselorIsSkipped() {
        service.create(dto(counselorId, rootId));
        User user = userMapper.selectById(userIdOf("13900003001"));
        user.setStatus(0);
        userMapper.updateById(user);

        assertThat(assigneeResolver.resolveUserId("COUNSELOR", studentId)).isNull();
    }

    @Test
    void previewCountsStudentsInSubtree() {
        assertThat(service.preview(rootId).get("studentCount")).isEqualTo(1L);
        assertThat(service.preview(childId).get("studentCount")).isEqualTo(1L);
    }

    private CounselorOrgSaveDTO dto(Long staffId, Long orgId) {
        CounselorOrgSaveDTO dto = new CounselorOrgSaveDTO();
        dto.setCounselorStaffId(staffId);
        dto.setOrgId(orgId);
        return dto;
    }

    private Long org(String name, Long parentId, String ancestors, int level) {
        Org org = new Org();
        org.setName(name);
        org.setParentId(parentId);
        org.setAncestors(ancestors);
        org.setLevel(level);
        org.setStatus(1);
        org.setCreatedAt(LocalDateTime.now());
        org.setUpdatedAt(LocalDateTime.now());
        orgMapper.insert(org);
        return org.getId();
    }

    private Long staff(String name, String personNo, String phone, PersonTypeEnum type, Long orgId) {
        Staff staff = new Staff();
        staff.setSchoolId(1L);
        staff.setName(name);
        staff.setPersonNo(personNo);
        staff.setPhone(phone);
        staff.setPersonType(type);
        staff.setOrgId(orgId);
        staff.setStatus(1);
        staff.setIsDeleted(0);
        staff.setSyncSource("LOCAL");
        staff.setCreatedAt(LocalDateTime.now());
        staff.setUpdatedAt(LocalDateTime.now());
        staffMapper.insert(staff);
        return staff.getId();
    }

    private Long userIdOf(String phone) {
        User user = userMapper.selectOne(new LambdaQueryWrapper<User>().eq(User::getPhone, phone));
        return user == null ? null : user.getId();
    }
}
