package com.visitor.workflow;

/**
 * 审批实例结束时发布，业务方据此更新单据状态（如访客记录、发凭证）。
 */
public record ApprovalCompletedEvent(Long instanceId, String bizType, Long bizId,
                                     Long schoolId, String status) {
}
