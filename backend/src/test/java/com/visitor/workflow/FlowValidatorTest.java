package com.visitor.workflow;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class FlowValidatorTest {

    private final FlowValidator validator = new FlowValidator();

    @Test
    void validFlowPasses() {
        FlowDefinition definition = new FlowDefinition("VISIT_STUDENT", "学生流程", 24, List.of(
                new FlowDefinition.Node(1, "学生审批", "TARGET", null),
                new FlowDefinition.Node(2, "辅导员审批", "COUNSELOR", null)));

        assertThat(validator.validate(definition)).isEmpty();
    }

    @Test
    void emptyNodesRejected() {
        FlowDefinition definition = new FlowDefinition("X", "空流程", 24, List.of());

        assertThat(validator.validate(definition)).anyMatch(error -> error.contains("至少需要一个审批节点"));
    }

    @Test
    void duplicateAndInvalidNodesRejected() {
        FlowDefinition definition = new FlowDefinition("X", "坏流程", 999, List.of(
                new FlowDefinition.Node(1, "A", "TARGET", null),
                new FlowDefinition.Node(1, "B", "UNKNOWN", null)));

        List<String> errors = validator.validate(definition);

        assertThat(errors).anyMatch(error -> error.contains("节点序号重复"));
        assertThat(errors).anyMatch(error -> error.contains("审批人类型不合法"));
        assertThat(errors).anyMatch(error -> error.contains("超时时间"));
    }

    @Test
    void codeAndNodeNameRequired() {
        FlowDefinition definition = new FlowDefinition("", "", null, List.of(
                new FlowDefinition.Node(1, "", "TARGET", null)));

        List<String> errors = validator.validate(definition);

        assertThat(errors).anyMatch(error -> error.contains("流程编码不能为空"));
        assertThat(errors).anyMatch(error -> error.contains("缺少名称"));
    }
}
