package com.visitor.domain.dto;

import com.alibaba.excel.annotation.ExcelProperty;
import lombok.Data;

/**
 * 白名单/黑名单导入行。类型列填写“黑名单”或“白名单”。
 */
@Data
public class AccessListImportRow {

    @ExcelProperty("姓名")
    private String personName;

    @ExcelProperty("手机号")
    private String phone;

    @ExcelProperty("车牌号")
    private String plateNumber;

    @ExcelProperty("类型")
    private String listType;

    @ExcelProperty("失效时间")
    private String effectiveTo;

    @ExcelProperty("原因")
    private String reason;
}
