package com.visitor.domain.dto;

import com.alibaba.excel.annotation.ExcelProperty;
import lombok.Data;

@Data
public class WhitelistImportRow {

    @ExcelProperty("姓名")
    private String personName;

    @ExcelProperty("手机号")
    private String phone;

    @ExcelProperty("车牌号")
    private String plateNumber;

    @ExcelProperty("生效时间")
    private String effectiveFrom;

    @ExcelProperty("失效时间")
    private String effectiveTo;

    @ExcelProperty("原因")
    private String reason;
}
