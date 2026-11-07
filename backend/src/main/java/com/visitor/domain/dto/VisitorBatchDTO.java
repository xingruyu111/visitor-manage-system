package com.visitor.domain.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 保卫处批量直录（无需审批，直接发证）。
 */
@Data
public class VisitorBatchDTO {

    @NotEmpty(message = "名单不能为空")
    @Valid
    private List<Item> items;

    @Data
    public static class Item {

        @NotBlank(message = "姓名不能为空")
        private String name;

        private String phone;
        private String idCard;
        private String visitPurpose;

        @NotNull(message = "开始时间不能为空")
        private LocalDateTime expectedTime;

        @NotNull(message = "结束时间不能为空")
        private LocalDateTime leaveTime;
    }
}
