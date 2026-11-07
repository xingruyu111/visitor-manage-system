package com.visitor.workflow;

import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * 流程定义校验：发布前必须通过。
 */
@Component
public class FlowValidator {

    private static final int MAX_NODES = 20;
    private static final int MAX_TIMEOUT_HOURS = 168;

    public List<String> validate(FlowDefinition definition) {
        List<String> errors = new ArrayList<>();
        if (definition == null) {
            return List.of("流程定义为空");
        }
        if (definition.code() == null || definition.code().isBlank()) {
            errors.add("流程编码不能为空");
        }
        if (definition.nodes() == null || definition.nodes().isEmpty()) {
            errors.add("至少需要一个审批节点");
            return errors;
        }
        if (definition.nodes().size() > MAX_NODES) {
            errors.add("节点数不能超过 " + MAX_NODES);
        }
        int timeout = definition.timeoutHours() == null ? 24 : definition.timeoutHours();
        if (timeout < 1 || timeout > MAX_TIMEOUT_HOURS) {
            errors.add("超时时间必须在 1–" + MAX_TIMEOUT_HOURS + " 小时之间");
        }

        Set<Integer> seqs = new HashSet<>();
        for (FlowDefinition.Node node : definition.nodes()) {
            if (node.seq() == null || node.seq() <= 0) {
                errors.add("节点序号必须为正整数");
                continue;
            }
            if (!seqs.add(node.seq())) {
                errors.add("节点序号重复: " + node.seq());
            }
            if (node.name() == null || node.name().isBlank()) {
                errors.add("节点 " + node.seq() + " 缺少名称");
            }
            if (AssigneeType.of(node.assignee()) == null) {
                errors.add("节点 " + node.seq() + " 的审批人类型不合法: " + node.assignee());
            }
        }
        if (seqs.stream().noneMatch(seq -> seq > 0)) {
            errors.add("缺少有效节点");
        }
        return errors;
    }
}
