package com.visitor.domain.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class SchoolClassSaveDTO {

    @NotBlank(message = "班级名称不能为空")
    private String name;

    private String grade;
    private Long headTeacherId;
    private Long counselorId;
}
