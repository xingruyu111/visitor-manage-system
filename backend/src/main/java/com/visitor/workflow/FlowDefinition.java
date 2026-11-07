package com.visitor.workflow;

import java.util.List;

/**
 * 审批流程定义（存于 vst_approval_flow.definition_json）。
 */
public record FlowDefinition(String code, String name, Integer timeoutHours, List<Node> nodes) {

    public record Node(Integer seq, String name, String assignee, Boolean optional) {

        public boolean isOptional() {
            return Boolean.TRUE.equals(optional);
        }
    }
}
