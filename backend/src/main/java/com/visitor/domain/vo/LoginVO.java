package com.visitor.domain.vo;

import lombok.Data;

import java.util.List;

@Data
public class LoginVO {

    private String token;
    private String tokenType = "Bearer";
    private long expiresIn;
    private Long userId;
    private String name;
    private String phone;
    private Integer currentRole;
    private Long schoolId;
    private List<RoleVO> roles;
}
