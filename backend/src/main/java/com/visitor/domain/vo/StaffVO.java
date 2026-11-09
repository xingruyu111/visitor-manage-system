package com.visitor.domain.vo;

import lombok.Data;

@Data
public class StaffVO {

    private Long id;
    private Long schoolId;
    private Long orgId;
    private String orgName;
    private Long deptId;
    private String deptName;
    private Long classId;
    private String className;
    private String name;
    private String phone;
    private String personNo;
    private Integer personType;
    private String personTypeDesc;
    private String jobNo;
    private String studentNo;
    private String position;
    private Long counselorId;
    private Integer status;
    private String syncSource;
    private String externalId;
}
