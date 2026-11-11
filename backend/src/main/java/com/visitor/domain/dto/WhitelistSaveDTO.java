package com.visitor.domain.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.time.LocalDateTime;

@Data
public class WhitelistSaveDTO {

    @NotBlank(message = "姓名不能为空")
    private String personName;

    @NotBlank(message = "手机号不能为空")
    private String phone;

    private String plateNumber;

    @NotNull(message = "生效时间不能为空")
    private LocalDateTime effectiveFrom;

    @NotNull(message = "失效时间不能为空")
    private LocalDateTime effectiveTo;

    private String reason;
}
