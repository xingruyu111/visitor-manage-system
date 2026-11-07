package com.visitor.security;

import com.visitor.common.enums.RoleEnum;
import com.visitor.common.exception.BizException;

/**
 * 学校数据范围：ADMIN 固定本校，SUPER 必须显式指定 schoolId。
 */
public final class SchoolScopes {

    private SchoolScopes() {
    }

    public static Long resolve(Long requestedSchoolId) {
        RoleEnum role = UserContext.getRole();
        if (role == RoleEnum.SYSTEM_ADMIN) {
            if (requestedSchoolId == null) {
                throw new BizException(400, "超级管理员需指定 schoolId");
            }
            return requestedSchoolId;
        }
        Long contextSchoolId = UserContext.getSchoolId();
        if (contextSchoolId == null) {
            throw new BizException(403, "缺少学校上下文");
        }
        if (requestedSchoolId != null && !contextSchoolId.equals(requestedSchoolId)) {
            throw new BizException(403, "无权操作其他学校的数据");
        }
        return contextSchoolId;
    }
}
