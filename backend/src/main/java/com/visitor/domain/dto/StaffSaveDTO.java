package com.visitor.domain.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class StaffSaveDTO {

    @NotBlank(message = "姓名不能为空")
    private String name;

    @NotBlank(message = "手机号不能为空")
    private String phone;

    @NotBlank(message = "工号/学号不能为空")
    private String personNo;
    private Integer personType;
    private Long deptId;
    private Long classId;
    private String jobNo;
    private String studentNo;
    private String position;
    private Long counselorId;
    private Integer status;
}
