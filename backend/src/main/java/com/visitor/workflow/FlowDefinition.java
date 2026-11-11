package com.visitor.workflow;

import java.util.List;

/**
 * 审批流程定义（存于 vst_approval_flow.definition_json）。
 */
public record FlowDefinition(String code, String name, Integer timeoutHours, List<Node> nodes,
                                  List<String> targetTypes) {

    public FlowDefinition(String code, String name, Integer timeoutHours, List<Node> nodes) {
        this(code, name, timeoutHours, nodes, List.of());
    }

    public record Node(Integer seq, String name, String assignee, Boolean optional) {

        public boolean isOptional() {
            return Boolean.TRUE.equals(optional);
        }
    }
}
