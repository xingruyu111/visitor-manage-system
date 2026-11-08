package com.visitor.domain.vo;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class VisitLogVO {

    private Long id;
    private Long recordId;
    private String direction;
    private String source;
    private String externalId;
    private LocalDateTime eventTime;
    private String visitorName;
    private String plateNumber;
}
