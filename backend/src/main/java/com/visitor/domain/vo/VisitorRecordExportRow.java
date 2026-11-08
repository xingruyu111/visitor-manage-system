package com.visitor.domain.vo;

import com.alibaba.excel.annotation.ExcelProperty;
import lombok.Data;

@Data
public class VisitorRecordExportRow {

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

    @ExcelProperty("通行方式")
    private String passMode;

    @ExcelProperty("车牌号")
    private String plateNumber;

    @ExcelProperty("到访时间")
    private String expectedTime;

    @ExcelProperty("离校时间")
    private String leaveTime;

    @ExcelProperty("状态")
    private String statusText;
}
