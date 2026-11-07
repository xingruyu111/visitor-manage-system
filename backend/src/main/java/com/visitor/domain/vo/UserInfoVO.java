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
    private List<RoleVO> roles;
}
