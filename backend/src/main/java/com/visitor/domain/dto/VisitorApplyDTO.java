package com.visitor.domain.dto;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.time.LocalDateTime;

@Data
public class VisitorApplyDTO {

    @NotNull(message = "被访人不能为空")
    private Long targetId;

    @NotNull(message = "预计到访时间不能为空")
    private LocalDateTime expectedTime;

    @NotNull(message = "预计离校时间不能为空")
    private LocalDateTime leaveTime;

    private String visitorType;
    private String visitPurpose;
    private String idCard;
}
