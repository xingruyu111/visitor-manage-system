package com.visitor.domain.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("vst_counselor_org")
public class CounselorOrg {

    @TableId(type = IdType.AUTO)
    private Long id;
    private Long schoolId;
    private Long counselorStaffId;
    private Long orgId;
    private Long createdBy;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
