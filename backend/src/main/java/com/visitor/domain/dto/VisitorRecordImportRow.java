package com.visitor.domain.dto;

import com.alibaba.excel.annotation.ExcelProperty;
import lombok.Data;

/**
 * 旧系统访客记录导入行。
 */
@Data
public class VisitorRecordImportRow {

    @ExcelProperty("访客姓名")
    private String visitorName;

    @ExcelProperty("手机号")
    private String visitorPhone;

    @ExcelProperty("证件号")
    private String idCard;

    @ExcelProperty("被访人")
    private String targetName;

    @ExcelProperty("事由")
    private String visitPurpose;

    @ExcelProperty("车牌号")
    private String plateNumber;

    @ExcelProperty("到访时间")
    private String expectedTime;

    @ExcelProperty("离校时间")
    private String leaveTime;

    @ExcelProperty("状态")
    private String status;
}
