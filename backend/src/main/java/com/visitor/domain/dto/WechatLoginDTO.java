package com.visitor.domain.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class WechatLoginDTO {

    @NotBlank(message = "code 不能为空")
    private String code;

    @NotBlank(message = "手机号不能为空")
    private String phone;
}
