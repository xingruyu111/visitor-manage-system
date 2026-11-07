package com.visitor.domain.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class StaffSaveDTO {

    @NotBlank(message = "姓名不能为空")
    private String name;

    private String phone;
    private Integer personType;
    private Long deptId;
    private Long classId;
    private String jobNo;
    private String studentNo;
    private String position;
    private Long counselorId;
    private Integer status;
}
