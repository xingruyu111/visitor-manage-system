package com.visitor.workflow;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.visitor.domain.entity.SchoolClass;
import com.visitor.domain.entity.Staff;
import com.visitor.domain.entity.User;
import com.visitor.server.mapper.SchoolClassMapper;
import com.visitor.server.mapper.StaffMapper;
import com.visitor.server.mapper.UserMapper;
import org.springframework.stereotype.Component;

/**
 * 把流程节点里的“审批人类型”解析为具体用户。
 */
@Component
public class AssigneeResolver {

    private final StaffMapper staffMapper;
    private final SchoolClassMapper classMapper;
    private final UserMapper userMapper;

    public AssigneeResolver(StaffMapper staffMapper, SchoolClassMapper classMapper, UserMapper userMapper) {
        this.staffMapper = staffMapper;
        this.classMapper = classMapper;
        this.userMapper = userMapper;
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

    private Long counselorIdOf(Long studentId) {
        Staff student = staffId(studentId);
        if (student == null) {
            return null;
        }
        if (student.getCounselorId() != null) {
            return student.getCounselorId();
        }
        SchoolClass schoolClass = student.getClassId() == null ? null : classMapper.selectById(student.getClassId());
        return schoolClass == null ? null : schoolClass.getCounselorId();
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
