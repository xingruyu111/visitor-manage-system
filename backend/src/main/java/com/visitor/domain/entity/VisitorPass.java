package com.visitor.domain.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("vst_visitor_pass")
public class VisitorPass {

    @TableId(type = IdType.AUTO)
    private Long id;
    private Long schoolId;
    private Long recordId;
    private String passNo;
    private String qrToken;
    private LocalDateTime validFrom;
    private LocalDateTime validTo;
    private Integer maxEntries;
    private Integer usedEntries;
    /** 0有效 1用尽 2过期 3撤销 */
    private Integer status;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
