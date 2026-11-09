package com.visitor.domain.dto;

import com.alibaba.excel.annotation.ExcelProperty;
import lombok.Data;

@Data
public class StaffImportRow {

    @ExcelProperty("工号/学号")
    private String personNo;

    @ExcelProperty("姓名")
    private String name;

    @ExcelProperty("手机号")
    private String phone;

    /** 名称或编码：教职工/学生/校内商户/食堂人员/附属学校教职工/附属学校学生/校友 */
    @ExcelProperty("人员类型")
    private String personType;

    @ExcelProperty("部门")
    private String deptName;

    @ExcelProperty("班级")
    private String className;

    @ExcelProperty("职务")
    private String position;
}
