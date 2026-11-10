package com.visitor.domain.dto;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class CounselorOrgSaveDTO {

    @NotNull(message = "请选择辅导员")
    private Long counselorStaffId;

    @NotNull(message = "请选择组织")
    private Long orgId;
}
