package com.visitor.workflow;

/**
 * 实例与任务状态常量。
 */
public final class ApprovalStatuses {

    public static final String RUNNING = "RUNNING";
    public static final String APPROVED = "APPROVED";
    public static final String REJECTED = "REJECTED";
    public static final String CANCELLED = "CANCELLED";

    public static final String PENDING = "PENDING";
    public static final String SKIPPED = "SKIPPED";
    public static final String EXPIRED = "EXPIRED";

    private ApprovalStatuses() {
    }
}
