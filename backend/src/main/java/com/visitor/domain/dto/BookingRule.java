package com.visitor.domain.dto;

import lombok.Data;

import java.util.List;

@Data
public class BookingRule {

    /** DAILY / WEEKLY / MONTHLY / MONTH_DAY / UNLIMITED */
    private String mode;
    private List<String> values;
    private String startTime;
    private String endTime;
}
