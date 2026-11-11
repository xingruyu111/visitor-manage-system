package com.visitor.domain.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.util.List;

@Data
public class VisitorTypeSaveDTO {

    @NotBlank(message = "类型编码不能为空")
    private String code;

    @NotBlank(message = "类型名称不能为空")
    private String name;

    @NotNull(message = "可通行次数不能为空")
    private Integer maxEntries;

    private List<BookingRule> bookingRules;

    private Integer status;

    private Integer sortNo;
}
