package com.visitor.domain.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class DepartmentSaveDTO {

    @NotBlank(message = "部门名称不能为空")
    private String name;

    private Long parentId;
    private Integer sortOrder;
}
