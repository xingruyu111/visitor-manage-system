package com.visitor.domain.dto;

import com.alibaba.excel.annotation.ExcelProperty;
import lombok.Data;

@Data
public class VisitorBatchImportRow {

    @ExcelProperty("姓名")
    private String name;

    @ExcelProperty("手机号")
    private String phone;

    @ExcelProperty("到访时间")
    private String expectedTime;

    @ExcelProperty("离校时间")
    private String leaveTime;

    @ExcelProperty("事由")
    private String visitPurpose;
}
