package com.visitor.domain.vo;

import lombok.Data;

@Data
public class SchoolClassVO {

    private Long id;
    private Long schoolId;
    private String name;
    private String grade;
    private Long headTeacherId;
    private Long counselorId;
    private Integer status;
}
