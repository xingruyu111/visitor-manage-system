package com.visitor.domain.vo;

import lombok.Data;

import java.util.List;

@Data
public class DepartmentVO {

    private Long id;
    private Long parentId;
    private String name;
    private Integer level;
    private Integer sortOrder;
    private Integer status;
    private List<DepartmentVO> children;
}
