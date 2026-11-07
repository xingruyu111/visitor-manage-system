package com.visitor.domain.vo;

import com.visitor.common.enums.RoleEnum;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class RoleVO {

    private Integer code;
    private String name;
    private Long schoolId;
    private Boolean defaultRole;

    public static RoleVO of(RoleEnum role, Long schoolId, boolean isDefault) {
        return new RoleVO(role.getCode(), role.getDescription(), schoolId, isDefault);
    }
}
