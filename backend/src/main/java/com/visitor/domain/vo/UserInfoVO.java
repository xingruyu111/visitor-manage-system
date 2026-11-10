package com.visitor.domain.vo;

import lombok.Data;

import java.util.List;

@Data
public class UserInfoVO {

    private Long id;
    private String phone;
    private String name;
    private String avatarUrl;
    private Integer currentRole;
    private Long schoolId;
    /** 人员类型：1教职工 2学生 3商户 4食堂 5附属教职工 6附属学生 7校友，纯访客为空 */
    private Integer personType;
    private String personTypeDesc;
    private List<RoleVO> roles;
}
