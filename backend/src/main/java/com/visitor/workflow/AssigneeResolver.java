package com.visitor.workflow;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.visitor.domain.entity.CounselorOrg;
import com.visitor.domain.entity.Org;
import com.visitor.domain.entity.SchoolClass;
import com.visitor.domain.entity.Staff;
import com.visitor.domain.entity.User;
import com.visitor.server.mapper.CounselorOrgMapper;
import com.visitor.server.mapper.OrgMapper;
import com.visitor.server.mapper.SchoolClassMapper;
import com.visitor.server.mapper.StaffMapper;
import com.visitor.server.mapper.UserMapper;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

/**
 * 把流程节点里的“审批人类型”解析为具体用户。
 */
@Component
public class AssigneeResolver {

    private final StaffMapper staffMapper;
    private final SchoolClassMapper classMapper;
    private final UserMapper userMapper;
    private final CounselorOrgMapper counselorOrgMapper;
    private final OrgMapper orgMapper;

    public AssigneeResolver(StaffMapper staffMapper, SchoolClassMapper classMapper, UserMapper userMapper,
                            CounselorOrgMapper counselorOrgMapper, OrgMapper orgMapper) {
        this.staffMapper = staffMapper;
        this.classMapper = classMapper;
        this.userMapper = userMapper;
        this.counselorOrgMapper = counselorOrgMapper;
        this.orgMapper = orgMapper;
    }

    /**
     * @param assigneeType TARGET / COUNSELOR / HEAD_TEACHER / SECURITY / DEFAULT
     * @param targetId     被访人（staff.id）
     * @return 审批人用户 id；无法解析时返回 null（由管理员兜底）
     */
    public Long resolveUserId(String assigneeType, Long targetId) {
        AssigneeType type = AssigneeType.of(assigneeType);
        if (type == null) {
            return null;
        }
        return switch (type) {
            case TARGET -> userIdOfStaff(targetId);
            case COUNSELOR -> userIdOfStaff(counselorIdOf(targetId));
            case HEAD_TEACHER -> userIdOfStaff(headTeacherIdOf(targetId));
            case SECURITY, DEFAULT -> null;
        };
    }

    public String nameOf(Long userId) {
        if (userId == null) {
            return null;
        }
        User user = userMapper.selectById(userId);
        return user == null ? null : user.getName();
    }

    /**
     * 辅导员解析顺序：个人指定 → 组织最近映射 → 班级辅导员。
     * 停用/删除的辅导员会被跳过，继续向上查找。
     */
    private Long counselorIdOf(Long studentId) {
        Staff student = staffId(studentId);
        if (student == null) {
            return null;
        }
        Long personal = activeCounselor(student.getCounselorId());
        if (personal != null) {
            return personal;
        }
        Long byOrg = counselorByOrg(student.getOrgId());
        if (byOrg != null) {
            return byOrg;
        }
        SchoolClass schoolClass = student.getClassId() == null ? null : classMapper.selectById(student.getClassId());
        return schoolClass == null ? null : activeCounselor(schoolClass.getCounselorId());
    }

    private Long counselorByOrg(Long orgId) {
        if (orgId == null) {
            return null;
        }
        List<Long> chain = new ArrayList<>();
        Org org = orgMapper.selectById(orgId);
        int guard = 0;
        while (org != null && guard++ < 30) {
            chain.add(org.getId());
            org = org.getParentId() == null ? null : orgMapper.selectById(org.getParentId());
        }
        for (Long id : chain) {
            CounselorOrg mapping = counselorOrgMapper.selectOne(new LambdaQueryWrapper<CounselorOrg>()
                    .eq(CounselorOrg::getOrgId, id)
                    .last("LIMIT 1"));
            if (mapping == null) {
                continue;
            }
            Long active = activeCounselor(mapping.getCounselorStaffId());
            if (active != null) {
                return active;
            }
        }
        return null;
    }

    /**
     * 只有人员在职且账号启用时才算有效辅导员。
     */
    private Long activeCounselor(Long staffId) {
        if (staffId == null) {
            return null;
        }
        Staff counselor = staffMapper.selectById(staffId);
        if (counselor == null || Integer.valueOf(1).equals(counselor.getIsDeleted())
                || !Integer.valueOf(1).equals(counselor.getStatus())
                || counselor.getPhone() == null || counselor.getPhone().isBlank()) {
            return null;
        }
        User user = userMapper.selectOne(new LambdaQueryWrapper<User>()
                .eq(User::getPhone, counselor.getPhone()).last("LIMIT 1"));
        if (user == null || !Integer.valueOf(1).equals(user.getStatus())) {
            return null;
        }
        return counselor.getId();
    }

    private Long headTeacherIdOf(Long studentId) {
        Staff student = staffId(studentId);
        if (student == null || student.getClassId() == null) {
            return null;
        }
        SchoolClass schoolClass = classMapper.selectById(student.getClassId());
        return schoolClass == null ? null : schoolClass.getHeadTeacherId();
    }

    private Staff staffId(Long staffId) {
        return staffId == null ? null : staffMapper.selectById(staffId);
    }

    private Long userIdOfStaff(Long staffId) {
        Staff staff = staffId(staffId);
        if (staff == null || staff.getPhone() == null || staff.getPhone().isBlank()) {
            return null;
        }
        User user = userMapper.selectOne(new LambdaQueryWrapper<User>()
                .eq(User::getPhone, staff.getPhone()));
        return user == null ? null : user.getId();
    }
}
