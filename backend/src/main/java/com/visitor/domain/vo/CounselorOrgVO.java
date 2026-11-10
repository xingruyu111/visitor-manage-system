package com.visitor.domain.vo;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class CounselorOrgVO {

    private Long id;
    private Long counselorStaffId;
    private String counselorName;
    private String counselorPersonNo;
    private String counselorPhone;
    private Long orgId;
    private String orgName;
    private Long studentCount;
    private LocalDateTime createdAt;
}
