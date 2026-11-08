package com.visitor.domain.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("vst_visitor_record")
public class VisitorRecord {

    @TableId(type = IdType.AUTO)
    private Long id;
    private Long schoolId;
    private Long visitorUserId;
    private String visitorName;
    private String visitorPhone;
    private String idCard;
    /** NORMAL / ALUMNI / PARENT / CONSTRUCTION / COOPERATION */
    private String visitorType;
    private String plateNumber;
    /** WALK/EBIKE/CAR */
    private String passMode;
    private String targetType;
    private Long targetId;
    private String targetName;
    private String visitPurpose;
    private LocalDateTime expectedTime;
    private LocalDateTime leaveTime;
    /** 0待审批 1已通过 2已拒绝 3已入校 4已离校 5已取消 6已过期 */
    private Integer status;
    private String extraJson;
    private String rejectReason;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
