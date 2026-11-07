package com.visitor.domain.dto;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.Map;

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

    /** 车牌号（机动车/电动自行车） */
    private String plateNumber;

    /** 差异化表单数据 */
    private Map<String, String> extra;
}
